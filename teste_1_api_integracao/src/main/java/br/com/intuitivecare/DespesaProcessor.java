package main.java.br.com.intuitivecare;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DespesaProcessor {

    public static void main(String[] args) {

        // pasta onde estão os CSV extraídos
        String pastaExtracted =
                "teste_1_api_integracao/data/extracted";

        // arquivo final
        String arquivoSaida =
                "teste_1_api_integracao/output/eventos_sinistros.csv";

        new File("teste_1_api_integracao/data/output").mkdirs();

        int contador = 0;
        boolean escreveuCabecalho = false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivoSaida))) {

            File pastaBase = new File(pastaExtracted);
            File[] pastas = pastaBase.listFiles();

           
            for (File pasta : pastas) {

                if (!pasta.isDirectory()) continue;

                File[] arquivos = pasta.listFiles();

                for (File arquivo : arquivos) {

                    if (!arquivo.getName().endsWith(".csv")) continue;

                    try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {

                        br.readLine(); // pular o cabeçalho

                        String linha;

                        while ((linha = br.readLine()) != null) {

                            
                            if (linha.contains("Eventos/Sinistros")) {

                                String[] valoresEntrePontoeVirgula = linha.split(";");

                                if (valoresEntrePontoeVirgula.length > 5) {
                                    writer.write(linha);
                                    writer.newLine();
                                    contador++;
                                }
                            }
                        }
                    }
                }
            }

            System.out.println("Total de linhas encontradas: " + contador);

        } catch (IOException e) {
            System.err.println("Erro ao ler os arquivos");
            e.printStackTrace();
        }
    }



    public void processarDespesa() {
        String arquivoFinalProcessado =
                "teste_1_api_integracao/data/output/eventos_sinistros.csv";


                /*TODO: FAZER A FILTRAGEM DOS REG ANS E MANDANDO PARA A API e na api retonar apenas o CNPJ , RazaoSocial , Trimestre , Ano , 
                    ValorDespesas desses reeg ans e salvar tudo em um CSV*/

                try {
            // 1. URL da API
            URL url = new URL("https://api.publicapis.org/entries");

            // 2. Abre conexão
            HttpURLConnection conexao = (HttpURLConnection) url.openConnection();

            // 3. Define método GET
            conexao.setRequestMethod("GET");

            // 4. Diz que aceita JSON
            conexao.setRequestProperty("Accept", "application/json");

            // 5. Código de resposta
            int status = conexao.getResponseCode();
            System.out.println("Status HTTP: " + status);

            // 6. Lê a resposta
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conexao.getInputStream())
            );

            String linha;
            StringBuilder resposta = new StringBuilder();

            while ((linha = br.readLine()) != null) {
                resposta.append(linha);
            }

            br.close();

            // 7. Mostra o retorno
            System.out.println(resposta.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    }


