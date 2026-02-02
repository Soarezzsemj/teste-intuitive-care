package main.java.br.com.intuitivecare.teste1;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Responsável por baixar e extrair dados de demonstrações contábeis da ANS.
 * Acessa o FTP da ANS, identifica trimestres disponíveis e extrai arquivos ZIP.
 */
public class AnsDownloader {

    public static void main(String[] args) {
        try {
            new AnsDownloader().baixarUltimosTrimestres(3);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Baixa os últimos N trimestres da ANS
    public void baixarUltimosTrimestres(int quantidade) throws Exception {
        System.out.println("=== Buscando últimos " + quantidade + " trimestres ===\n");
        
        List<String> todosOsTrimestres = new ArrayList<>();
        int[] anos = {2025, 2024, 2023, 2022, 2021};
        
        // Varre os últimos anos procurando por trimestres disponíveis
        for (int ano : anos) {
            System.out.println("Verificando: " + ano);
            String urlAno = "https://dadosabertos.ans.gov.br/FTP/PDA/demonstracoes_contabeis/" + ano + "/";
            
            try {
                String html = lerURL(urlAno);
                
                // Procura por arquivos no padrão 1T2025.zip, 2Q2025.zip e esses ai
                Matcher m = Pattern.compile("href=[\"']?([0-9][TQ][0-9]{4}\\.zip)[\"']?", Pattern.CASE_INSENSITIVE).matcher(html);
                
                while (m.find()) {
                    String zip = m.group(1);
                    todosOsTrimestres.add(urlAno + zip);
                    System.out.println("  Encontrado: " + zip);
                }
            } catch (Exception e) {
                System.out.println("  Ano não disponível");
            }
        }
        
        if (todosOsTrimestres.isEmpty()) {
            System.out.println("\n Nenhum trimestre encontrado!");
            return;
        }
        
        // Ordena do mais recente pro mais antigo para pegar os últimos N
        Collections.sort(todosOsTrimestres, Collections.reverseOrder());
        
        System.out.println("\n=== Baixando os " + quantidade + " mais recentes ===\n");
        
        int baixados = 0;
        // Itera pelos trimestres e baixa os N mais recentes
        for (String url : todosOsTrimestres) {
            if (baixados >= quantidade) break;
            
            String nome = url.substring(url.lastIndexOf('/') + 1);
            System.out.println("Baixando: " + nome);
            baixarArquivo(url);
            baixados++;
        }
        
        // Após baixar, extrai todos os ZIPs
        System.out.println("\n=== Extraindo ===");
        extrairTodos();
        System.out.println("Concluído! " + baixados + " trimestres baixados\n");
    }

    // Lê HTML de uma URL
    private String lerURL(String url) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        StringBuilder html = new StringBuilder();
        String linha;
        while ((linha = in.readLine()) != null) html.append(linha);
        in.close();
        return html.toString();
    }

    // Baixa um arquivo ZIP da ANS se ainda não existir localmente
    private void baixarArquivo(String url) throws Exception {
        String nome = url.substring(url.lastIndexOf('/') + 1);
        String destino = "teste_1_api_integracao/data/raw/" + nome;
        
        // Se arquivo já existe, não baixa novamente
        File arquivo = new File(destino);
        if (arquivo.exists()) {
            System.out.println("  Já existe, pulando");
            return;
        }
        
        new File("teste_1_api_integracao/data/raw").mkdirs();
        
        // Copia o arquivo da URL para o disco
        InputStream in = new URL(url).openStream();
        OutputStream out = new FileOutputStream(destino);
        
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        
        in.close();
        out.close();
        System.out.println("  Baixado");
    }

    // Extrai todos os ZIPs da pasta raw para extracted
    private void extrairTodos() {
        File pasta = new File("teste_1_api_integracao/data/raw");
        // Filtra apenas arquivos .zip
        File[] zips = pasta.listFiles((d, n) -> n.endsWith(".zip"));
        
        if (zips == null) return;
        
        for (File zip : zips) {
            String destino = "teste_1_api_integracao/data/extracted/" + zip.getName().replace(".zip", "");
            File pastaDestino = new File(destino);
            
            // Se já foi extraído, pula
            if (pastaDestino.exists() && pastaDestino.list() != null && pastaDestino.list().length > 0) {
                System.out.println("Já extraído: " + zip.getName());
                continue;
            }
            
            pastaDestino.mkdirs();
            System.out.println("Extraindo: " + zip.getName());
            
            // Abre o ZIP e extrai cada arquivo para a pasta de destino
            try (ZipInputStream zin = new ZipInputStream(new FileInputStream(zip))) {
                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    File arquivo = new File(destino, entry.getName());
                    
                    if (entry.isDirectory()) {
                        arquivo.mkdirs();
                    } else {
                        arquivo.getParentFile().mkdirs();
                        OutputStream out = new FileOutputStream(arquivo);
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = zin.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        out.close();
                    }
                    zin.closeEntry();
                }
            } catch (Exception e) {
                System.out.println("Erro: " + zip.getName());
            }
        }
    }
}