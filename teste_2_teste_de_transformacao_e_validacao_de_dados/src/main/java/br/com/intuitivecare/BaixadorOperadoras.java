package main.java.br.com.intuitivecare;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Baixa o arquivo de operadoras ativas do ANS
 * URL: https://dadosabertos.ans.gov.br/FTP/PDA/operadoras_de_plano_de_saude_ativas/
 * 
 * Procura por arquivo CSV com padrão de operadoras ativas
 * Extrai para data/raw/operadoras.csv
 */
public class BaixadorOperadoras {
    
    /**
     * Baixa arquivo da URL
     */
    private static InputStream baixarDoServidor(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
        conexao.setConnectTimeout(10000);
        conexao.setReadTimeout(10000);
        conexao.setRequestProperty("User-Agent", "Mozilla/5.0");
        
        if (conexao.getResponseCode() != 200) {
            throw new Exception("Erro ao conectar: " + conexao.getResponseCode());
        }
        
        return conexao.getInputStream();
    }
    
    /**
     * Lista arquivos disponíveis na URL HTML e procura por Relatorio_cadop.csv
     */
    private static String procurarArquivoOperadoras() throws Exception {
        String url = "https://dadosabertos.ans.gov.br/FTP/PDA/operadoras_de_plano_de_saude_ativas/";
        
        System.out.println("Acessando: " + url);
        
        try (InputStream input = baixarDoServidor(url);
             BufferedReader leitor = new BufferedReader(new InputStreamReader(input))) {
            
            String linha;
            String arquivoCSV = null;
            
            // Procura especificamente por Relatorio_cadop.csv
            while ((linha = leitor.readLine()) != null) {
                if (linha.contains("Relatorio_cadop") && linha.contains(".csv")) {
                    // Extrai nome do arquivo do HTML
                    int inicio = linha.indexOf("href=\"") + 6;
                    int fim = linha.indexOf("\"", inicio);
                    String arquivo = linha.substring(inicio, fim);
                    
                    if (arquivo.contains(".csv")) {
                        arquivoCSV = arquivo;
                        System.out.println("Arquivo encontrado: " + arquivoCSV);
                        break;
                    }
                }
            }
            
            return arquivoCSV;
        }
    }
    
    /**
     * Baixa arquivo CSV direto (sem necessidade de extrair ZIP)
     */
    private static void baixarCSVDireto(String urlArquivo, String caminhoSaida) throws Exception {
        System.out.println("Baixando: " + urlArquivo);
        
        try (InputStream input = baixarDoServidor(urlArquivo);
             FileOutputStream fos = new FileOutputStream(caminhoSaida)) {
            
            byte[] buffer = new byte[1024];
            int lido;
            while ((lido = input.read(buffer)) != -1) {
                fos.write(buffer, 0, lido);
            }
            
            System.out.println("Arquivo salvo em: " + caminhoSaida);
            
            // Mostra cabeçalho para debug
            try (BufferedReader br = new BufferedReader(new FileReader(caminhoSaida))) {
                String cabecalho = br.readLine();
                System.out.println("Colunas: " + cabecalho);
            }
        }
    }
    
    /**
     * Orquestra download do arquivo CSV
     */
    public static void main(String[] args) {
        String caminhoSaida = "teste_2_teste_de_transformacao_e_validacao_de_dados\\data\\raw\\operadoras.csv";
        new java.io.File(caminhoSaida).getParentFile().mkdirs();
        
        try {
            // Procura arquivo disponível
            String nomeArquivo = procurarArquivoOperadoras();
            if (nomeArquivo == null) {
                System.out.println("[ERRO] Nenhum arquivo Relatorio_cadop encontrado");
                return;
            }
            
            // Monta URL completa
            String urlCompleta = "https://dadosabertos.ans.gov.br/FTP/PDA/operadoras_de_plano_de_saude_ativas/" + nomeArquivo;
            
            // Baixa CSV direto
            baixarCSVDireto(urlCompleta, caminhoSaida);
            
        } catch (Exception e) {
            System.out.println("[ERRO] ao baixar operadoras: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
