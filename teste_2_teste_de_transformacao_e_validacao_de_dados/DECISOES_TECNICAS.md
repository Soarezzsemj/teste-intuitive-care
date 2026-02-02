# DECISOES TECNICAS - TESTE 2

## 1. Validação de Dados (2.1)

### Decisão: Estratégia MARCAR vs REJEITAR
**Escolhido: MARCAR (Não rejeita, marca com [INVALIDO])**

#### Justificativa
- Preserva dados para análise e auditoria posterior
- Permite identificar padrões de erros (CNPJs inválidos, valores negativos)
- Facilita relatório de qualidade de dados
- Não perde informação (pode ser corrigida manualmente depois)

#### Alternativas Consideradas

| Estratégia | Prós | Contras |
|-----------|------|---------|
| **REJEITAR** | Garante 100% de qualidade | Perde dados; difícil auditoria; pode mascarar problemas sistêmicos |
| **MARCAR**  | Preserva dados; identifica problemas; flexível | Requer processamento extra de registros inválidos |
| **CORRIGIR AUTOMATICAMENTE** | Mantém volume de dados | Arriscado (pode causar erros maiores); pouca rastreabilidade |

### Implementação: Validação de Valores
**Decisão: Aceitar valores negativos (variações contábeis)**
- Valores negativos são **válidos** (provisões, devoluções, ajustes contábeis)
- Validação: Apenas verifica se é numérico (não rejeita <= 0)
- Justificativa: Conjunto de dados contém variações de provisão (valor < 0)

### Implementação: Validação CNPJ
Algoritmo de dígitos verificadores:
- Valida 14 dígitos obrigatórios
- Rejeita sequências repetidas (00000000000000)
- Calcula dois dígitos verificadores usando multiplicadores decrescentes
- Aceita CNPJs com ou sem formatação

### Trade-off: Complexidade vs Cobertura
**Decisão: Validação completa (dígitos verificadores)**
- Justificativa: CNPJ é ID crítico; falha nela causa erros de join em passos subsequentes
- Custo: ~20 linhas de código extra, negligenciável em performance

---

## 2. Enriquecimento de Dados (2.2)

### Decisão: Estratégia de Processamento (In-Memory vs Streaming)
**Escolhido: In-Memory HashMap**

#### Justificativa
- Arquivo de operadoras: ~10K registros (estimado)
- Consolidado: 747 registros
- Precisa fazer múltiplas buscas (um lookup por cada registro consolidado)
  
**Análise de I/O:**
- **Streaming**: 747 leituras do arquivo operadoras = 747 × 10K = ~7,47M operações
- **In-Memory**: 1 leitura completa (10K) + 747 lookups HashMap = ~750 operações
- **Economia: ~10.000x menos I/O**

#### Overhead de Memória
- 10K operadoras × ~200 bytes = ~2MB máximo
- Negligenciável em máquinas modernas

### Tratamento de Mismatches

#### CNPJ não encontrado no cadastro
- **Marcação**: `[SEM_MATCH]` nas colunas RegistroANS
- **Causa**: CNPJ inválido validado no passo 1, ou CNPJ novo após data do cadastro
- **Ação**: Marca mas mantém registro (permite análise posterior)

#### CNPJ duplicado no cadastro
- **Marcação**: Usa primeiro registro encontrado (ordem do arquivo)
- **Causa**: Operadora com múltiplas entradas (filiais? erros de entrada?)
- **Ação**: Log de aviso, mas processa normalmente

### Download de Operadoras
- **Abordagem**: Procura e baixa arquivo `Relatorio_cadop.csv` direto da URL
  - URL: `https://dadosabertos.ans.gov.br/FTP/PDA/operadoras_de_plano_de_saude_ativas/`
  - Arquivo: CSV com dados cadastrais (CNPJ, RegistroANS, Modalidade, UF)
- **Tratamento de erro**: Se nenhum arquivo encontrado, permite execução manual
- **Timeout**: 10 segundos (evita travamento)

---

## 3. Agregação de Dados (2.3)

### Decisão: Estratégia de Ordenação
**Escolhido: TreeMap com Comparator customizado**

#### Justificativa por Volume
- Operadoras/UF únicas: ~737 (estimado, menos de 1000)
- **TreeMap**: O(log n) por inserção, mantém ordenação automaticamente
- **ArrayList + sort**: O(n log n) mas requer passo separado
- Para volumes < 10K, diferença é negligenciável; TreeMap é mais legível

#### Implementação
```java
// Comparator ordena por valor total descrescente (maior para menor)
public int compareTo(EstatisticasOperadora outro) {
    return Double.compare(outro.totalDespesas, this.totalDespesas);
}
```

### Cálculos Estatísticos

#### Total de Despesas
- Soma simples de todos os valores do trimestre para operadora/UF

#### Média
- `Total / Contagem` (média aritmética simples)
- Todos os registros têm peso igual

#### Média por Trimestre
- `Total / ContagemTrimestresUnicos`
- Normaliza por quantidade de trimestres com dados
- Evita viés de operadoras com mais trimestres

#### Desvio Padrão
- `sqrt(soma((valor - media)^2) / n)`
- Desvio padrão populacional (não amostral)
- Identifica variabilidade (operadoras com despesas muito flutuantes)

### Trade-off: Velocidade vs Flexibilidade
**Decisão: Manter dados em memória durante agregação**

| Aspecto | In-Memory | Streaming |
|---------|-----------|-----------|
| **Desvio Padrão** | Direto (têm todos os valores) | Complexo (requer algoritmo streaming) |
| **Memória** | ~747 operadoras × valores | Negligenciável |
| **I/O** | 1 leitura | 1 leitura |
| **Código** | Simples, legível | Complexo, Welford algorithm |

**Justificativa**: Volume pequeno; trade-off não relevante; código mais legível

---

## 4. Arquitetura Geral

### Pipeline 4-Passos

```
Teste 1 (consolidado_despesas.csv)
        ↓
[ValidadorDados] → consolidado_despesas_validado.csv
        ↓
[BaixadorOperadoras] → operadoras.csv (FTP ANS)
        ↓
[EnriquecedorDados] → consolidado_despesas_enriquecido.csv
        ↓
[AgregadorDespesas] → despesas_agregadas.csv
```

### Decisão: Componentes Separados vs Monolítico
**Escolhido: 4 classes separadas + Main orquestradora**

#### Justificativa
- **Legibilidade**: Cada classe tem responsabilidade única
- **Testabilidade**: Pode executar cada passo isoladamente
- **Manutenibilidade**: Fácil modificar um passo sem afetar outros
- **Reutilização**: ValidadorDados pode ser usado em outros contextos

### Tratamento de Erros
- **Try-catch abrangente**: Não para o pipeline; log de erros
- **Registros inválidos**: Marca e continua (não rejeita)
- **Falhas de download**: Falha graciosamente, instrui execução manual
- **Divisão por zero**: Verificação (p.ex., `if (trimestresUnicos.isEmpty()) return 0`)

---

## 5. Performance Estimada

| Operação | Registros | Tempo Estimado |
|----------|-----------|----------------|
| Validar 747 CNPJs | 747 | ~100ms |
| Download operadoras | 1 | ~2-5s (rede) |
| Carregar 10K operadoras | 10K | ~100ms |
| Join 747 × 10K | 747 | ~50ms |
| Agregação TreeMap | 747 | ~20ms |
| **Total** | - | **~2.3-6s** |

Tempo dominado por download (variável conforme conexão).

---

## 6. Conformidade com Requisitos

- ✅ 2.1: Validação CNPJ, valores, razão social
- ✅ 2.1: Decisão técnica documentada (MARCAR)
- ✅ 2.2: Download de operadoras do FTP
- ✅ 2.2: Join por CNPJ
- ✅ 2.2: Adiciona RegistroANS, Modalidade, UF
- ✅ 2.2: Tratamento de mismatches documentado
- ✅ 2.3: Agrupamento por RazaoSocial + UF
- ✅ 2.3: Total de despesas
- ✅ 2.3: Média por trimestre
- ✅ 2.3: Desvio padrão
- ✅ 2.3: Ordenação por valor (maior para menor)
- ✅ 2.3: Trade-off documentado
- ✅ Saída: despesas_agregadas.csv
