package main.java.br.com.intuitivecare;

import java.io.*;
import java.util.*;

/**
 * Agrega dados de despesas por Razão Social e UF
 * Calcula:
 * - Total de despesas
 * - Média de despesas por trimestre
 * - Desvio padrão das despesas
 * - Conta de registros
 * 
 * Estratégia de ordenação: TreeMap com Comparator customizado
 * Justificativa: Volume pequeno (737 operadoras únicas estimadas)
 * TreeMap já mantém ordenação e economiza passo separado de sort
 * 
 * Saída: despesas_agregadas.csv ordenado por valor total (maior para menor)
 */
public class AgregadorDespesas {
    
    /**
     * Classe interna para armazenar estatísticas por operadora/UF
     */
    static class EstatisticasOperadora implements Comparable<EstatisticasOperadora> {
        String razaoSocial;
        String uf;
        double totalDespesas = 0;
        int contagem = 0;
        List<Double> despesas = new ArrayList<>(); // Para cálculo de desvio padrão
        Set<String> trimestresUnicos = new HashSet<>(); // Para média por trimestre
        
        EstatisticasOperadora(String razaoSocial, String uf) {
            this.razaoSocial = razaoSocial;
            this.uf = uf;
        }
        
        void adicionarDespesa(double valor, String trimestre) {
            totalDespesas += valor;
            despesas.add(valor);
            contagem++;
            trimestresUnicos.add(trimestre);
        }
        
        double getMedia() {
            return totalDespesas / contagem;
        }
        
        double getMediaPorTrimestre() {
            if (trimestresUnicos.isEmpty()) return 0;
            return totalDespesas / trimestresUnicos.size();
        }
        
        double getDesviaoPadrao() {
            if (despesas.size() < 2) return 0;
            
            double media = getMedia();
            double soma = 0;
            for (double valor : despesas) {
                soma += Math.pow(valor - media, 2);
            }
            return Math.sqrt(soma / despesas.size());
        }
        
        // Comparator para ordenar por valor total (maior para menor)
        @Override
        public int compareTo(EstatisticasOperadora outro) {
            return Double.compare(outro.totalDespesas, this.totalDespesas);
        }
    }
    
    /**
     * Lê arquivo consolidado/enriquecido e agrega por RazaoSocial/UF
     */
    public static void main(String[] args) {
        String caminhoEntrada = "teste_2_teste_de_transformacao_e_validacao_de_dados\\output\\consolidado_despesas_enriquecido.csv";
        String caminhoSaida = "teste_2_teste_de_transformacao_e_validacao_de_dados\\output\\despesas_agregadas.csv";
        new java.io.File(caminhoSaida).getParentFile().mkdirs();
        
        // Mapa para agrupar estatísticas (chave: "RazaoSocial;UF")
        Map<String, EstatisticasOperadora> agregacoes = new TreeMap<>();
        
        int linhasProcessadas = 0;
        int linhasIgnoradas = 0;
        
        try (BufferedReader leitor = new BufferedReader(new FileReader(caminhoEntrada))) {
            String linha;
            String[] cabecalho = null;
            
            while ((linha = leitor.readLine()) != null) {
                String[] campos = linha.split(";");
                
                if (cabecalho == null) {
                    cabecalho = campos;
                    System.out.println("Cabeçalho: " + String.join(", ", campos));
                    System.out.println("Total de colunas: " + campos.length);
                    continue;
                }
                
                // Debug: mostra primeiras 3 linhas
                if (linhasProcessadas < 3 || linhasIgnoradas < 3) {
                    System.out.println("Linha " + (linhasProcessadas + linhasIgnoradas + 1) + ": " + 
                        (campos.length >= 2 ? campos[1] : "???") + " | Colunas: " + campos.length);
                }
                
                // Esperado: CNPJ;RazaoSocial;Trimestre;Ano;ValorDespesas;RegistroANS;Modalidade;UF
                if (campos.length < 8) {
                    linhasIgnoradas++;
                    continue;
                }
                
                try {
                    String razaoSocial = campos[1].trim();
                    String trimestre = campos[2].trim();
                    // O valor está na coluna 4 (ValorDespesas)
                    // O valor pode ser negativo (variações de provisões são válidas)
                    String valorStr = campos[4].replace(",", ".").trim();
                    double valor = Double.parseDouble(valorStr);
                    
                    // Tenta pegar UF (coluna 7), se vazio ou [SEM_MATCH], usa "DESCONHECIDO"
                    String ufTemp = "";
                    if (campos.length > 7) {
                        ufTemp = campos[7].trim();
                    }
                    if (ufTemp.isEmpty() || ufTemp.contains("[SEM_MATCH]")) {
                        ufTemp = "DESCONHECIDO";
                    }
                    final String uf = ufTemp;
                    
                    // Ignora apenas registros completamente inválidos
                    // Aceita valores negativos (são variações contábeis válidas)
                    if (razaoSocial.isEmpty()) {
                        linhasIgnoradas++;
                        continue;
                    }
                    
                    // Chave única por operadora/UF
                    String chave = razaoSocial + ";" + uf;
                    
                    EstatisticasOperadora stats = agregacoes.computeIfAbsent(chave,
                        k -> new EstatisticasOperadora(razaoSocial, uf));
                    
                    stats.adicionarDespesa(valor, trimestre);
                    linhasProcessadas++;
                    
                } catch (Exception e) {
                    linhasIgnoradas++;
                    System.out.println("[ERRO na linha] " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            System.out.println("[ERRO] ao ler arquivo: " + e.getMessage());
            return;
        }
        
        // Ordena por valor total (maior para menor)
        List<EstatisticasOperadora> ordenadas = new ArrayList<>(agregacoes.values());
        Collections.sort(ordenadas); // Usa compareTo implementado
        
        // Escreve resultado agregado
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(caminhoSaida))) {
            // Escreve cabeçalho
            String cabecalhoSaida = "RazaoSocial;UF;TotalDespesas;Media;MediaPorTrimestre;DesviaoPadrao;Contagem;TrimestresUnicos";
            escritor.write(cabecalhoSaida);
            escritor.newLine();
            
            // Escreve dados agregados (já ordenados)
            for (EstatisticasOperadora stats : ordenadas) {
                String linha = String.format("%s;%s;%.2f;%.2f;%.2f;%.2f;%d;%d",
                    stats.razaoSocial,
                    stats.uf,
                    stats.totalDespesas,
                    stats.getMedia(),
                    stats.getMediaPorTrimestre(),
                    stats.getDesviaoPadrao(),
                    stats.contagem,
                    stats.trimestresUnicos.size()
                );
                escritor.write(linha);
                escritor.newLine();
            }
            
            System.out.println("\n=== AGREGACAO CONCLUIDA ===");
            System.out.println("Linhas processadas: " + linhasProcessadas);
            System.out.println("Linhas ignoradas: " + linhasIgnoradas);
            System.out.println("Operadoras/UF unicas: " + ordenadas.size());
            System.out.println("Arquivo gerado: " + caminhoSaida);
            
            // Mostra top 5
            System.out.println("\nTop 5 Operadoras por Despesa Total:");
            for (int i = 0; i < Math.min(5, ordenadas.size()); i++) {
                EstatisticasOperadora stats = ordenadas.get(i);
                System.out.printf("[%d] %s (%s): R$ %.2f%n",
                    i + 1, stats.razaoSocial, stats.uf, stats.totalDespesas);
            }
            
        } catch (IOException e) {
            System.out.println("[ERRO] ao escrever agregação: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
