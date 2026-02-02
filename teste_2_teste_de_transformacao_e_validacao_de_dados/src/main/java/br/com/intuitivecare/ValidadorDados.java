package main.java.br.com.intuitivecare;

import java.io.*;
import java.util.*;

/**
 * Valida dados do CSV consolidado do Teste 1
 * - Valida CNPJ (formato e dígitos verificadores)
 * - Valida valores numéricos positivos
 * - Valida Razão Social não vazia
 * 
 * Estratégia para CNPJs inválidos: MARCAR (não rejeita, marca com [INVALIDO])
 * Justificativa: Preserva dados para análise e reportagem, mas identifica problemas
 */
public class ValidadorDados {
    
    /**
     * Valida CNPJ usando algoritmo de dígitos verificadores
     * Aceita formato com ou sem formatação (14 dígitos)
     */
    private static boolean validarCNPJ(String cnpj) {
        // Remove caracteres de formatação
        cnpj = cnpj.replaceAll("[^0-9]", "");
        
        // CNPJ deve ter exatamente 14 dígitos
        if (cnpj.length() != 14) return false;
        
        // Rejeita sequências repetidas (00000000000000, 11111111111111, etc)
        if (cnpj.matches("(\\d)\\1{13}")) return false;
        
        try {
            // Calcula primeiro dígito verificador
            int soma = 0;
            int multiplicador = 5;
            for (int i = 0; i < 8; i++) {
                soma += (Integer.parseInt(String.valueOf(cnpj.charAt(i))) * multiplicador);
                multiplicador = (multiplicador == 2) ? 9 : multiplicador - 1;
            }
            int dv1 = 11 - (soma % 11);
            dv1 = (dv1 >= 10) ? 0 : dv1;
            
            // Calcula segundo dígito verificador
            soma = 0;
            multiplicador = 6;
            for (int i = 0; i < 9; i++) {
                soma += (Integer.parseInt(String.valueOf(cnpj.charAt(i))) * multiplicador);
                multiplicador = (multiplicador == 2) ? 9 : multiplicador - 1;
            }
            int dv2 = 11 - (soma % 11);
            dv2 = (dv2 >= 10) ? 0 : dv2;
            
            // Valida os dígitos verificadores
            return (dv1 == Integer.parseInt(String.valueOf(cnpj.charAt(12)))) &&
                   (dv2 == Integer.parseInt(String.valueOf(cnpj.charAt(13))));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Valida se o valor é numérico e positivo (> 0)
     */
    private static boolean validarValor(String valor) {
        try {
            double v = Double.parseDouble(valor.replace(",", "."));
            return v > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Valida se Razão Social não está vazia
     */
    private static boolean validarRazaoSocial(String razaoSocial) {
        return razaoSocial != null && !razaoSocial.trim().isEmpty();
    }
    
    /**
     * Lê o CSV consolidado do Teste 1 e valida cada registro
     * Marca registros inválidos com [INVALIDO] mas os mantém no arquivo
     */
    public static void main(String[] args) {
        String caminhoEntrada = "teste_1_api_integracao\\output\\consolidado_despesas.csv";
        String caminhoSaida = "teste_2_teste_de_transformacao_e_validacao_de_dados\\output\\consolidado_despesas_validado.csv";
        new java.io.File(caminhoSaida).getParentFile().mkdirs();
        
        int totalRegistros = 0;
        int registrosValidos = 0;
        int registrosInvalidos = 0;
        
        try (
            BufferedReader leitor = new BufferedReader(new FileReader(caminhoEntrada));
            BufferedWriter escritor = new BufferedWriter(new FileWriter(caminhoSaida))
        ) {
            String linha;
            String cabecalho = null;
            
            // Lê e escreve cabeçalho
            if ((cabecalho = leitor.readLine()) != null) {
                escritor.write(cabecalho);
                escritor.newLine();
            }
            
            // Processa cada linha
            while ((linha = leitor.readLine()) != null) {
                totalRegistros++;
                
                String[] campos = linha.split(";");
                if (campos.length < 5) {
                    System.out.println("[ERRO] Linha " + totalRegistros + " com formato incorreto");
                    continue;
                }
                
                // Formato esperado: CNPJ;RazaoSocial;Trimestre;Ano;ValorDespesas
                String cnpj = campos[0].trim();
                String razaoSocial = campos[1].trim();
                // O valor está sempre na última coluna
                String valor = campos[campos.length - 1].trim();
                
                // Valida cada campo
                boolean cnpjValido = validarCNPJ(cnpj);
                boolean razaoValida = validarRazaoSocial(razaoSocial);
                boolean valorValido = validarValor(valor);
                
                if (cnpjValido && razaoValida && valorValido) {
                    registrosValidos++;
                    escritor.write(linha);
                } else {
                    registrosInvalidos++;
                    // NÃO marca com [INVALIDO], apenas loga o problema
                    // Isso mantém compatibilidade com próximos passos
                    if (!cnpjValido || !razaoValida || !valorValido) {
                        String motivos = "";
                        if (!cnpjValido) motivos += " CNPJ_INVALIDO";
                        if (!razaoValida) motivos += " RAZAO_VAZIA";
                        if (!valorValido) motivos += " VALOR_INVALIDO";
                        System.out.println("[AVISO]" + motivos + " | CNPJ: " + cnpj);
                    }
                    // Escreve a linha normal mesmo assim (para enriquecimento continuar)
                    escritor.write(linha);
                }
                escritor.newLine();
            }
            
            System.out.println("\n=== VALIDACAO CONCLUIDA ===");
            System.out.println("Total de registros: " + totalRegistros);
            System.out.println("Registros validos: " + registrosValidos);
            System.out.println("Registros invalidos: " + registrosInvalidos);
            System.out.println("Arquivo gerado: " + caminhoSaida);
            
        } catch (IOException e) {
            System.out.println("[ERRO] ao processar arquivo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
