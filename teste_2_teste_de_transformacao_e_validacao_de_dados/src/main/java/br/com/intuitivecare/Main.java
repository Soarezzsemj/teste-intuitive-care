package main.java.br.com.intuitivecare;

/**
 * Orquestra o pipeline completo do Teste 2:
 * Validação → Enriquecimento → Agregação
 * 
 * PASSO 1: ValidadorDados
 *   - Lê consolidado_despesas.csv do Teste 1
 *   - Valida CNPJ, valores, razão social
 *   - Marca registros inválidos mas os mantém
 *   - Output: consolidado_despesas_validado.csv
 * 
 * PASSO 2: BaixadorOperadoras
 *   - Baixa arquivo de operadoras do FTP ANS
 *   - Extrai CSV com dados cadastrais
 *   - Output: data/raw/operadoras.csv
 * 
 * PASSO 3: EnriquecedorDados
 *   - Carrega operadoras em memória (HashMap)
 *   - Faz join por CNPJ com consolidado_despesas.csv
 *   - Adiciona: RegistroANS, Modalidade, UF
 *   - Marca CNPJs sem match
 *   - Output: consolidado_despesas_enriquecido.csv
 * 
 * PASSO 4: AgregadorDespesas
 *   - Agrega por RazaoSocial;UF
 *   - Calcula: Total, Média, Desvio Padrão, Média/Trimestre
 *   - Ordena por valor total (maior para menor)
 *   - Output: despesas_agregadas.csv
 */
public class Main {
    
    public static void main(String[] args) {
        System.out.println("=== TESTE 2: Transformacao e Validacao de Dados ===\n");
        
        System.out.println("PASSO 1: Validando dados consolidados...");
        ValidadorDados.main(null);
        
        System.out.println("\n\nPASSO 2: Baixando dados de operadoras...");
        BaixadorOperadoras.main(null);
        
        System.out.println("\n\nPASSO 3: Enriquecendo dados com informacoes cadastrais...");
        EnriquecedorDados.main(null);
        
        System.out.println("\n\nPASSO 4: Agregando despesas por operadora/UF...");
        AgregadorDespesas.main(null);
        
        System.out.println("\n\n=== PIPELINE COMPLETO CONCLUIDO ===");
        System.out.println("Arquivos gerados:");
        System.out.println("  - output\\consolidado_despesas_validado.csv");
        System.out.println("  - data\\raw\\operadoras.csv");
        System.out.println("  - output\\consolidado_despesas_enriquecido.csv");
        System.out.println("  - output\\despesas_agregadas.csv");
        System.out.println("  - DECISOES_TECNICAS.md");
        System.out.println("  - RELATORIO_CONFORMIDADE.md");
    }
}
