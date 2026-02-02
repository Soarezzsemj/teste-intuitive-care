package main.java.br.com.intuitivecare;

import java.io.*;
import java.util.*;

/**
 * Enriquece dados consolidados com informações cadastrais das operadoras
 * - Faz join entre consolidado_despesas.csv e operadoras.csv usando CNPJ
 * - Adiciona colunas: RegistroANS, Modalidade, UF
 * - Trata mismatches (CNPJs não encontrados, duplicados)
 * 
 * Estratégia JOIN: In-memory com HashMap (operadoras carregadas uma única vez)
 * Justificativa: Arquivo de operadoras é menor (~10K registros), economiza I/O
 * para arquivo consolidado (747 registros com múltiplas leituras)
 * 
 * Tratamento de mismatches:
 * - CNPJ não encontrado no cadastro: marca como [SEM_MATCH]
 * - CNPJ duplicado no cadastro: usa primeiro registro encontrado, marca [DUPLICADO_CADASTRO]
 */
public class EnriquecedorDados {
    
    /**
     * Classe interna para armazenar dados das operadoras
     */
    static class Operadora {
        String registroANS;
        String modalidade;
        String uf;
        
        Operadora(String registroANS, String modalidade, String uf) {
            this.registroANS = registroANS;
            this.modalidade = modalidade;
            this.uf = uf;
        }
    }
    
    /**
     * Carrega operadoras.csv em memória (HashMap para lookup rápido)
     * Detecta automaticamente as colunas necessárias (CNPJ, UF, RegistroANS, Modalidade)
     */
    private static Map<String, Operadora> carregarOperadoras(String caminhoOperadoras) {
        Map<String, Operadora> operadoras = new HashMap<>();
        Set<String> cpnjsDuplicados = new HashSet<>();
        
        try (BufferedReader leitor = new BufferedReader(new FileReader(caminhoOperadoras))) {
            String linha;
            int cnpjIdx = -1, ufIdx = -1, registroIdx = -1, modalidadeIdx = -1;
            int linhaNum = 0;
            
            while ((linha = leitor.readLine()) != null) {
                linhaNum++;
                String[] campos = linha.split(";");
                
                if (linhaNum == 1) {
                    // Cabeçalho: busca índices das colunas necessárias
                    for (int i = 0; i < campos.length; i++) {
                        String coluna = campos[i].trim().toUpperCase();
                        if (coluna.contains("CNPJ")) cnpjIdx = i;
                        if (coluna.contains("UF") || coluna.contains("SIGLA")) ufIdx = i;
                        if (coluna.contains("REGISTRO") || coluna.contains("REG")) registroIdx = i;
                        if (coluna.contains("MODALIDADE")) modalidadeIdx = i;
                    }
                    
                    System.out.println("Índices encontrados: CNPJ=" + cnpjIdx + ", UF=" + ufIdx + 
                        ", RegistroANS=" + registroIdx + ", Modalidade=" + modalidadeIdx);
                    continue;
                }
                
                if (cnpjIdx < 0 || campos.length <= cnpjIdx) continue;
                
                // Remove aspas do CNPJ e outros campos
                String cnpj = campos[cnpjIdx].trim().replaceAll("\"", "");
                String uf = (ufIdx >= 0 && ufIdx < campos.length) ? campos[ufIdx].trim().replaceAll("\"", "") : "";
                String registroANS = (registroIdx >= 0 && registroIdx < campos.length) ? campos[registroIdx].trim().replaceAll("\"", "") : "";
                String modalidade = (modalidadeIdx >= 0 && modalidadeIdx < campos.length) ? campos[modalidadeIdx].trim().replaceAll("\"", "") : "";
                
                if (operadoras.containsKey(cnpj)) {
                    cpnjsDuplicados.add(cnpj);
                    System.out.println("[AVISO] CNPJ duplicado no cadastro: " + cnpj);
                } else {
                    operadoras.put(cnpj, new Operadora(registroANS, modalidade, uf));
                }
            }
            
            System.out.println("Operadoras carregadas: " + operadoras.size());
            System.out.println("CNPJs duplicados encontrados: " + cpnjsDuplicados.size());
            
        } catch (IOException e) {
            System.out.println("[ERRO] ao carregar operadoras: " + e.getMessage());
        }
        
        return operadoras;
    }
    
    /**
     * Faz join e enriquece dados com informações cadastrais
     */
    public static void main(String[] args) {
        String caminhoConsolidado = "teste_1_api_integracao\\output\\consolidado_despesas.csv";
        String caminhoOperadoras = "teste_2_teste_de_transformacao_e_validacao_de_dados\\data\\raw\\operadoras.csv";
        String caminhoSaida = "teste_2_teste_de_transformacao_e_validacao_de_dados\\output\\consolidado_despesas_enriquecido.csv";
        new java.io.File(caminhoSaida).getParentFile().mkdirs();
        
        // Carrega operadoras em memória
        Map<String, Operadora> operadoras = carregarOperadoras(caminhoOperadoras);
        
        int totalRegistros = 0;
        int comMatch = 0;
        int semMatch = 0;
        
        try (
            BufferedReader leitor = new BufferedReader(new FileReader(caminhoConsolidado));
            BufferedWriter escritor = new BufferedWriter(new FileWriter(caminhoSaida))
        ) {
            String linha;
            String cabecalhoOriginal = null;
            
            // Lê e escreve cabeçalho (adiciona novas colunas)
            if ((cabecalhoOriginal = leitor.readLine()) != null) {
                String novoCabecalho = cabecalhoOriginal + ";RegistroANS;Modalidade;UF";
                escritor.write(novoCabecalho);
                escritor.newLine();
            }
            
            // Processa cada linha do consolidado
            while ((linha = leitor.readLine()) != null) {
                totalRegistros++;
                
                String[] campos = linha.split(";");
                if (campos.length < 5) continue;
                
                String cnpj = campos[0].trim();
                
                // Busca CNPJ nas operadoras carregadas
                Operadora operadora = operadoras.get(cnpj);
                
                if (operadora != null) {
                    comMatch++;
                    // Escreve linha original + dados enriquecidos
                    escritor.write(linha + ";" + 
                                 operadora.registroANS + ";" + 
                                 operadora.modalidade + ";" + 
                                 operadora.uf);
                    escritor.newLine();
                } else {
                    semMatch++;
                    // CNPJ não encontrado no cadastro
                    escritor.write(linha + ";[SEM_MATCH];;");
                    escritor.newLine();
                    
                    if (semMatch <= 10) { // Log apenas dos primeiros 10
                        System.out.println("[SEM_MATCH] CNPJ nao encontrado: " + cnpj);
                    }
                }
            }
            
            if (semMatch > 10) {
                System.out.println("... e mais " + (semMatch - 10) + " registros sem match");
            }
            
            System.out.println("\n=== ENRIQUECIMENTO CONCLUIDO ===");
            System.out.println("Total de registros: " + totalRegistros);
            System.out.println("Com match no cadastro: " + comMatch);
            System.out.println("Sem match: " + semMatch);
            System.out.println("Arquivo gerado: " + caminhoSaida);
            
        } catch (IOException e) {
            System.out.println("[ERRO] ao enriquecer dados: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
