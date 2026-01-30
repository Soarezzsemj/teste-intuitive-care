package main.java.br.com.intuitivecare;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AnsDownloader {

    private static final String BASE_URL = "https://dadosabertos.ans.gov.br/FTP/PDA/demonstracoes_contabeis/";
    private static final String PASTA_RAW = "teste_1_api_integracao/data/raw";
    private static final String PASTA_EXTRAIDA = "teste_1_api_integracao/data/extracted";

    public static void main(String[] args) {
        AnsDownloader downloader = new AnsDownloader();
        
        try {
            downloader.processarAno(2025);
        } catch (IOException e) {
            System.err.println("Erro: " + e.getMessage());
        }
    }

    /**
     * Processa um ano: busca URLs, baixa e extrai
     */
    public void processarAno(int ano) throws IOException {
        System.out.println("=== Processando ano " + ano + " ===\n");
        
        List<String> urls = buscarURLs(BASE_URL + ano + "/");
        System.out.println("Encontrados: " + urls.size() + " arquivo(s)\n");
        
        baixarArquivos(urls);
        extrairZips();
        
        System.out.println("\n✓ Processo concluído!");
    }

    /**
     * Busca URLs de arquivos .zip na página
     */
    private List<String> buscarURLs(String pageUrl) throws IOException {
        List<String> urls = new ArrayList<>();
        String html = baixarHTML(pageUrl);
        
        Pattern pattern = Pattern.compile("href=[\"']([^\"']*\\.zip)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        
        while (matcher.find()) {
            String url = matcher.group(1);
            urls.add(url.startsWith("http") ? url : pageUrl + url);
        }
        
        return urls;
    }

    /**
     * Baixa o HTML de uma página
     */
    private String baixarHTML(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        
        StringBuilder html = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                html.append(linha);
            }
        }
        return html.toString();
    }

    /**
     * Baixa múltiplos arquivos
     */
    private void baixarArquivos(List<String> urls) {
        for (String url : urls) {
            try {
                baixarArquivo(url);
            } catch (IOException e) {
                System.err.println("Erro ao baixar: " + url);
            }
        }
    }

    /**
     * Baixa um arquivo
     */
    private void baixarArquivo(String urlString) throws IOException {
        String nomeArquivo = urlString.substring(urlString.lastIndexOf('/') + 1);
        String caminhoSaida = PASTA_RAW + "/" + nomeArquivo;
        
        new File(PASTA_RAW).mkdirs();
        System.out.println("Baixando: " + nomeArquivo);
        
        try (InputStream in = new URL(urlString).openStream();
             OutputStream out = new FileOutputStream(caminhoSaida)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            
            System.out.println("✓ Concluído: " + nomeArquivo);
        }
    }

    /**
     * Extrai todos os arquivos .zip
     */
    private void extrairZips() {
        File pasta = new File(PASTA_RAW);
        File[] zips = pasta.listFiles((dir, nome) -> nome.endsWith(".zip"));
        
        if (zips == null || zips.length == 0) {
            System.out.println("Nenhum .zip encontrado");
            return;
        }
        
        System.out.println("\nExtraindo arquivos...");
        for (File zip : zips) {
            extrairZip(zip);
        }
    }

    /**
     * Extrai um arquivo .zip
     */
    private void extrairZip(File zipFile) {
        String pastaDestino = PASTA_EXTRAIDA + "/" + zipFile.getName().replace(".zip", "");
        new File(pastaDestino).mkdirs();
        
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                File arquivo = new File(pastaDestino, entry.getName());
                
                if (entry.isDirectory()) {
                    arquivo.mkdirs();
                } else {
                    arquivo.getParentFile().mkdirs();
                    try (OutputStream out = new FileOutputStream(arquivo)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = zip.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zip.closeEntry();
            }
            System.out.println("✓ Extraído: " + zipFile.getName());
        } catch (IOException e) {
            System.err.println("Erro ao extrair: " + zipFile.getName());
        }
    }
}