package main.java.br.com.intuitivecare;

/**
 * Orquestrador principal do processamento de dados ANS.
 * Executa o pipeline completo: download, extração, filtragem e consolidação.
 */
public class Main {

    public static void main(String[] args) {

        System.out.println("=================================");
        System.out.println("  INICIANDO PROCESSAMENTO ANS");
        System.out.println("=================================\n");

        // PASSO 1: Baixa dados de demonstrações contábeis da ANS
        System.out.println("PASSO 1: Baixando arquivos dos últimos 3 trimestres...\n");
        AnsDownloader.main(null);

        // PASSO 2: Filtra eventos/sinistros e consulta API para dados consolidados
        System.out.println("\n\nPASSO 2: Extraindo eventos de sinistros e consultando API...\n");
        DespesaProcessor.main(null);

        System.out.println("\n=================================");
        System.out.println("  PROCESSAMENTO CONCLUIDO!");
        System.out.println("=================================");
        System.out.println("\nArquivos gerados:");
        System.out.println("  - teste_1_api_integracao/data/extracted/ (arquivos extraídos dos ZIPs)");
        System.out.println("  - teste_1_api_integracao/output/eventos_sinistros.csv");
        System.out.println("  - teste_1_api_integracao/output/consolidado_despesas.csv");
        System.out.println("  - RELATORIO_CONFORMIDADE.md");
        System.out.println("  - DECISOES_TECNICAS.md\n");
    }
}