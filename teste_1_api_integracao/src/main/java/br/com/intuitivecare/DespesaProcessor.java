package main.java.br.com.intuitivecare;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Processa dados de eventos/sinistros extraídos e consolida com dados da API ANS.
 * Filtra registros específicos, consulta API para obter CNPJ/Razão Social,
 * e detecta inconsistências nos dados (CNPJs duplicados, valores problemáticos).
 */
public class DespesaProcessor {

    public static void main(String[] args) {
        System.out.println("Iniciando processamento...\n");

        String pastaExtracted = "teste_1_api_integracao/data/extracted";
        String arquivoSaida = "teste_1_api_integracao/output/eventos_sinistros.csv";

        new File("teste_1_api_integracao/output").mkdirs();

        int contador = 0;

        // Lê todos os CSVs extraídos e filtra apenas linhas com "Eventos/Sinistros"
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivoSaida))) {

            File pastaBase = new File(pastaExtracted);
            File[] pastas = pastaBase.listFiles();

            if (pastas == null) {
                System.out.println("Pasta extracted nao encontrada!");
                return;
            }

            // Itera pelas pastas de trimestres (1T2025, 2T2025)
            for (File pasta : pastas) {
                if (!pasta.isDirectory()) continue;

                File[] arquivos = pasta.listFiles();
                if (arquivos == null) continue;

                // Processa cada arquivo CSV da pasta
                for (File arquivo : arquivos) {
                    if (!arquivo.getName().endsWith(".csv")) continue;

                    System.out.println("Lendo: " + arquivo.getName());

                    try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
                        br.readLine(); // Pula header

                        String linha;
                        // Filtra apenas linhas que contêm "Eventos/Sinistros"
                        while ((linha = br.readLine()) != null) {
                            if (linha.contains("Eventos/Sinistros")) {
                                String[] colunas = linha.split(";");
                                if (colunas.length > 5) {
                                    writer.write(linha);
                                    writer.newLine();
                                    contador++;
                                }
                            }
                        }
                    }
                }
            }

            System.out.println("Total: " + contador + " linhas\n");

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Após gerar arquivo de eventos, processa RegANS e consulta API
        System.out.println("Processando Reg ANS...\n");
        new DespesaProcessor().pegarRegANS();
    }

    // Lê arquivo de eventos, agrupa por RegANS e consulta API para consolidar dados
    public void pegarRegANS() {
        File arquivoCSV = new File("teste_1_api_integracao/output/eventos_sinistros.csv");
        Map<String, List<RegistroSinistro>> registrosPorRegANS = new HashMap<>();
        Set<String> cpnjsDuplicados = new HashSet<>();
        int valoresProblematicos = 0;

        try {
            Scanner scanner = new Scanner(arquivoCSV);

            // Lê arquivo e agrupa registros por RegANS (identificador da operadora)
            while (scanner.hasNextLine()) {
                String linha = scanner.nextLine();
                String[] colunas = linha.split(";");

                if (colunas.length < 6) continue;

                String regANS = colunas[1].trim().replace("\"", "");
                String trimestre = colunas[2].trim().replace("\"", "");
                String ano = colunas[3].trim().replace("\"", "");
                String valorDespesas = colunas[5].trim().replace("\"", "");

                // Valida se RegANS é numérico
                if (!regANS.matches("\\d+")) continue;

                // Detecta valores problemáticos (zero ou negativos)
                try {
                    double valor = Double.parseDouble(valorDespesas.replace(",", "."));
                    if (valor <= 0) {
                        valoresProblematicos++;
                        System.out.println("  [AVISO] Valor " + (valor == 0 ? "ZERO" : "NEGATIVO") + ": " + valorDespesas);
                    }
                } catch (NumberFormatException e) {
                    // Ignora erros de parsing
                }

                RegistroSinistro registro = new RegistroSinistro(regANS, trimestre, ano, valorDespesas);
                registrosPorRegANS.putIfAbsent(regANS, new ArrayList<>());
                registrosPorRegANS.get(regANS).add(registro);
            }

            scanner.close();

            System.out.println("Reg ANS unicos: " + registrosPorRegANS.size());
            System.out.println("Valores problematicos (zero/negativo): " + valoresProblematicos + "\n");

            int processados = 0;
            int sucessos = 0;

            // Para cada RegANS único, consulta API ANS para obter CNPJ e Razão Social
            for (Map.Entry<String, List<RegistroSinistro>> entry : registrosPorRegANS.entrySet()) {
                String regANS = entry.getKey();
                List<RegistroSinistro> registros = entry.getValue();

                processados++;
                System.out.println("[" + processados + "/" + registrosPorRegANS.size() + "] Reg ANS: " + regANS);

                DadosOperadora dados = buscarNaAPI(regANS);

                // Grava cada registro com dados da API
                for (RegistroSinistro reg : registros) {
                    String cnpj = dados != null ? dados.cnpj : "N/A";
                    
                    // Detecta CNPJs duplicados (mesma operadora com registros diferentes)
                    if (dados != null && cpnjsDuplicados.contains(cnpj)) {
                        System.out.println("  [DUPLICADO] CNPJ: " + cnpj);
                    } else if (dados != null) {
                        cpnjsDuplicados.add(cnpj);
                    }

                    salvarCSV(
                        cnpj,
                        dados != null ? dados.razaoSocial : "N/A",
                        reg.trimestre,
                        reg.ano,
                        reg.valorDespesas
                    );
                }

                if (dados != null) sucessos++;
            }

            // Relatório final de processamento
            System.out.println("\nProcessados: " + processados);
            System.out.println("Sucessos API: " + sucessos);
            System.out.println("CNPJs unicos: " + cpnjsDuplicados.size() + "\n");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Consulta API da ANS para obter dados da operadora (CNPJ, Razão Social)
    private DadosOperadora buscarNaAPI(String registroANS) {
        try {
            String urlDaAPI = "https://www.ans.gov.br/operadoras-entity/v1/operadoras/" + registroANS;
            URL url = new URL(urlDaAPI);

            HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
            conexao.setRequestMethod("GET");
            conexao.setRequestProperty("Accept", "application/json");
            conexao.setConnectTimeout(5000);
            conexao.setReadTimeout(5000);

            int status = conexao.getResponseCode();

            if (status == 200) {
                // Lê resposta JSON da API
                BufferedReader br = new BufferedReader(new InputStreamReader(conexao.getInputStream()));
                StringBuilder resposta = new StringBuilder();
                String linha;
                while ((linha = br.readLine()) != null) {
                    resposta.append(linha);
                }
                br.close();
                conexao.disconnect();

                String json = resposta.toString();
                // Extrai campos CNPJ e razao_social do JSON
                String cnpj = extrairJSON(json, "\"cnpj\":\"");
                String razaoSocial = extrairJSON(json, "\"razao_social\":\"");

                System.out.println("  OK: " + cnpj + " - " + razaoSocial);
                return new DadosOperadora(cnpj, razaoSocial);
            } else {
                System.out.println("  Erro: " + status);
                return null;
            }

        } catch (Exception e) {
            System.out.println("  Erro: " + e.getMessage());
            return null;
        }
    }

    // Extrai valor de um campo JSON simples (sem usar library JSON)
    private String extrairJSON(String json, String chave) {
        try {
            int inicio = json.indexOf(chave);
            if (inicio == -1) return "N/A";
            inicio += chave.length();
            int fim = json.indexOf("\"", inicio);
            if (fim == -1) return "N/A";
            return json.substring(inicio, fim);
        } catch (Exception e) {
            return "N/A";
        }
    }

    // Grava registro consolidado no CSV final (append mode)
    private void salvarCSV(String cnpj, String razaoSocial, String trimestre, String ano, String valorDespesas) {
        try {
            File arquivoSaida = new File("teste_1_api_integracao/output/consolidado_despesas.csv");
            boolean jaExiste = arquivoSaida.exists();

            FileWriter fw = new FileWriter(arquivoSaida, true);
            BufferedWriter bw = new BufferedWriter(fw);

            // Escreve header na primeira vez
            if (!jaExiste) {
                bw.write("CNPJ;RazaoSocial;Trimestre;Ano;ValorDespesas");
                bw.newLine();
            }

            // Grava linha com dados consolidados
            String linha = cnpj + ";" + razaoSocial + ";" + trimestre + ";" + ano + ";" + valorDespesas;
            bw.write(linha);
            bw.newLine();

            bw.close();
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Classe interna para armazenar um registro de evento/sinistro
    static class RegistroSinistro {
        String regANS;
        String trimestre;
        String ano;
        String valorDespesas;

        RegistroSinistro(String regANS, String trimestre, String ano, String valorDespesas) {
            this.regANS = regANS;
            this.trimestre = trimestre;
            this.ano = ano;
            this.valorDespesas = valorDespesas;
        }
    }

    // Classe interna para armazenar dados obtidos da API
    static class DadosOperadora {
        String cnpj;
        String razaoSocial;

        DadosOperadora(String cnpj, String razaoSocial) {
            this.cnpj = cnpj;
            this.razaoSocial = razaoSocial;
        }
    }
}
