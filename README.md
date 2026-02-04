# Intuitive Care - Sistema de Análise de Despesas de Operadoras

**Nível**: Estagiário/Junior | **Linguagens**: Java, Python, SQL | **Status**: Completo

---

## Tabela de Conteúdos

1. [Visão Geral](#visão-geral)
2. [Como Executar](#como-executar)
3. [Estrutura do Projeto](#estrutura-do-projeto)
4. [Trade-offs Técnicos](#trade-offs-técnicos)
5. [Testes Implementados](#testes-implementados)
6. [Notas de Implementação](#notas-de-implementação)

---

## Visão Geral

Este projeto implementa um **pipeline completo de análise de despesas** de operadoras de saúde, extraindo dados da API pública da ANS, validando, enriquecendo e apresentando em dashboard interativo.

**Fluxo Geral:**
```
Teste 1: Download ANS → Consolidação CSV
              ↓
Teste 2: Validação → Enriquecimento → Agregação
              ↓
Teste 3: Importação SQL → Queries Analíticas
              ↓
Teste 4: API FastAPI + Frontend Vue.js
```

---

## Como Executar

### Pré-requisitos
- Java 11+
- Python 3.9+
- MySQL 8.0 ou PostgreSQL 10+
- Node.js 16+

### Teste 1: Integração com API ANS

```bash
cd teste_1_api_integracao
javac -d bin src/main/java/br/com/intuitivecare/teste1/*.java
java -cp bin br.com.intuitivecare.teste1.Main

# Outputs:
# - data/extracted/     (arquivos extraídos dos ZIPs)
# - output/consolidado_despesas.csv
```

**O que faz:**
- Baixa os 3 últimos trimestres da API ANS
- Extrai ZIPs automaticamente
- Filtra apenas Eventos/Sinistros
- Consolida em CSV com colunas: CNPJ, RazaoSocial, Trimestre, Ano, ValorDespesas
- Detecta e loga inconsistências (duplicados, valores zerados/negativos)

---

### Teste 2: Transformação e Validação

```bash
cd teste_2_teste_de_transformacao_e_validacao_de_dados
javac -d bin src/main/java/br/com/intuitivecare/*.java
java -cp bin br.com.intuitivecare.Main

# Outputs:
# - output/consolidado_despesas_validado.csv
# - output/consolidado_despesas_enriquecido.csv
# - output/despesas_agregadas.csv
```

**O que faz:**
- PASSO 1: Valida CNPJs (dígitos verificadores), valores, razão social
- PASSO 2: Baixa dados de operadoras ativas
- PASSO 3: Faz JOIN por CNPJ adicionando RegistroANS, Modalidade, UF
- PASSO 4: Agrega por RazaoSocial/UF com Total, Média, Desvio Padrão

---

### Teste 3: Banco de Dados e SQL

```bash
cd teste_3_banco_de_dados_e_analise

# Criar banco de dados (MySQL)
mysql -u root -p < 01_ddl_tabelas.sql
mysql -u root -p < 02_importacao_csv.sql
mysql -u root -p < 03_queries_analiticas.sql
```

**Queries Implementadas:**
- Query 1: 5 operadoras com maior crescimento percentual
- Query 2: Distribuição de despesas por UF (Top 5)
- Query 3: Operadoras acima da média em 2+ trimestres
```

---

### Teste 4: API Web

```bash
# Backend (FastAPI)
cd teste_4_api_e_interface_web/backend
pip install -r requirements.txt
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# API em: http://localhost:8000
# Docs: http://localhost:8000/docs

# Frontend (Vue.js)
cd ../frontend
npm install
npm run dev

# Frontend em: http://localhost:5173
```

**Rotas da API:**
- `GET /api/operadoras?page=1&limit=10&search=` - Lista com paginação
- `GET /api/operadoras/{cnpj}` - Detalhes de operadora
- `GET /api/operadoras/{cnpj}/despesas` - Histórico de despesas
- `GET /api/estatisticas` - Estatísticas agregadas (total, média, top 5)

---

## Estrutura do Projeto

```
teste-intuitive-care/
│
├── teste_1_api_integracao/
│   ├── src/main/java/br/com/intuitivecare/teste1/
│   │   ├── Main.java                  # Orquestrador
│   │   ├── AnsDownloader.java         # Download de trimestres
│   │   └── DespesaProcessor.java      # Processamento e consolidação
│   ├── data/
│   │   ├── raw/                       # ZIPs baixados
│   │   └── extracted/                 # ZIPs extraídos
│   ├── output/
│   │   ├── consolidado_despesas.csv   # CSV final
│   │   └── eventos_sinistros.csv      # Intermediário
│   └── DECISOES_TECNICAS.md           # Trade-offs documentados
│
├── teste_2_teste_de_transformacao_e_validacao_de_dados/
│   ├── src/main/java/br/com/intuitivecare/
│   │   ├── Main.java                  # Orquestrador pipeline
│   │   ├── ValidadorDados.java        # Validação CNPJ/valores
│   │   ├── BaixadorOperadoras.java    # Download operadoras
│   │   ├── EnriquecedorDados.java     # Join com operadoras
│   │   └── AgregadorDespesas.java     # Agregação e ordenação
│   ├── data/raw/
│   │   └── operadoras.csv             # Dados cadastrais
│   ├── output/
│   │   ├── consolidado_despesas_validado.csv
│   │   ├── consolidado_despesas_enriquecido.csv
│   │   └── despesas_agregadas.csv
│   └── DECISOES_TECNICAS.md
│
├── teste_3_banco_de_dados_e_analise/
│   ├── 01_ddl_tabelas.sql             # Criação de tabelas
│   ├── 02_importacao_csv.sql          # LOAD DATA scripts
│   └── 03_queries_analiticas.sql      # 3 queries analíticas
│
├── teste_4_api_e_interface_web/
│   ├── backend/
│   │   ├── app/
│   │   │   └── main.py                # FastAPI com 4 rotas
│   │   └── requirements.txt
│   ├── frontend/
│   │   ├── src/
│   │   │   ├── App.vue                # Dashboard + tabela
│   │   │   ├── main.js
│   │   │   └── style.css
│   │   ├── package.json
│   │   └── vite.config.js
│   └── README.md
│
└── README.md (ESTE ARQUIVO)
```

---

## Trade-offs Técnicos (Justificativas)

### TESTE 1: API Integration

#### Trade-off 1.1: Processamento Incremental vs Em Memória

**ESCOLHA**: Processamento Incremental (Streaming com BufferedReader)

**JUSTIFICATIVA:**
- Arquivos de demonstrações contábeis podem ter **centenas de milhões de linhas**
- Carregamento completo em memória causaria `OutOfMemoryError`
- Streaming permite processar arquivos de qualquer tamanho
- Footprint de memória é **constante** (não cresce com tamanho do arquivo)

**TRADE-OFF:**
- **Vantagem**: Escalabilidade, segurança, recursos previsíveis
- **Desvantagem**: Processamento mais lento, sem reorganização complexa em tempo real

**APLICAÇÃO NO CÓDIGO:**
```java
// Streaming line-by-line em vez de carregar tudo
try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
    String linha;
    while ((linha = br.readLine()) != null) {
        // Processa linha a linha
    }
}
```

---

#### Trade-off 1.2: Identificação Automática de Estrutura

**ESCOLHA**: String Matching Simples (nível estagiário)

**JUSTIFICATIVA:**
- Projeto é **nível estagiário**, não precisa de parser sophisticado
- Identifica padrões: `"Eventos/Sinistros"` funciona para os dados disponíveis
- Implementação: ~5 linhas de código
- Maintível e compreensível para iniciantes

**LIMITAÇÃO DOCUMENTADA:**
- Assume apenas CSV (não suporta XLSX, TXT)
- Estrutura de colunas pode variar, mas código é resiliente

**CAMINHO PARA MELHORIA:**
- Apache Commons CSV para parsing mais robusto
- Apache POI para XLSX
- Temporal para depois (não é bloqueador)

---

#### Trade-off 1.3: Consolidação com Validação Básica

**ESCOLHA**: Marcar Inconsistências (não rejeitar dados)

**JUSTIFICATIVA:**
- Preserva dados para análise e auditoria posterior
- Permite identificar padrões de erros (CNPJs duplicados, valores negativos)
- Facilita relatório de qualidade
- Não perde informação

**IMPLEMENTAÇÃO:**
```
[DUPLICADO] CNPJ: xxxxx → marca, mas processa
[AVISO] Valor NEGATIVO: xxxxx → marca, mas processa
[AVISO] Valor ZERO: xxxxx → marca, mas processa
```

---

### TESTE 2: Transformação e Validação

#### Trade-off 2.1: Estratégia para CNPJs Inválidos

**ESCOLHA**: MARCAR (não rejeita, marca com `[INVALIDO]`)

**JUSTIFICATIVA:**

| Estratégia | Prós | Contras |
|-----------|------|---------|
| REJEITAR | Garante 100% qualidade | Perde dados; difícil auditoria |
| MARCAR | Preserva dados; identifica padrões | Requer processamento extra |
| CORRIGIR AUTOMATICAMENTE | Mantém volume | Arriscado; pouca rastreabilidade |

**ESCOLHIDO**: MARCAR por permitir análise posterior

---

#### Trade-off 2.2: In-Memory HashMap vs Streaming para Join

**ESCOLHA**: In-Memory HashMap (carregar operadoras em memória)

**JUSTIFICATIVA COM CÁLCULOS:**

```
Arquivo de operadoras: ~10K registros (estimado)
Consolidado: 747 registros
Operação: 747 joins

STREAMING (busca arquivo inteiro 747 vezes):
  747 buscas × 10K = ~7.47 MILHÕES de operações

IN-MEMORY (carrega 1x, 747 lookups em memória):
  1 leitura (10K) + 747 lookups HashMap = ~750 operações

ECONOMIA: ~10.000x MENOS I/O
```

**OVERHEAD DE MEMÓRIA:**
- 10K operadoras × ~200 bytes = ~2MB máximo
- Negligenciável em máquinas modernas

**DECISÃO:** In-Memory é 10.000x mais eficiente

---

#### Trade-off 2.3: Validação de Valores Positivos

**ESCOLHA**: Aceitar Valores Negativos (provisões contábeis)

**JUSTIFICATIVA:**
- Requisito diz "positivos", mas dados reais contêm negativos
- Negativos são **válidos** em contabilidade (devoluções, provisões)
- Validação: Apenas verifica se é numérico
- Rejeitar levaria a perda de dados importante

**DOCUMENTADO EM**: DECISOES_TECNICAS.md (justificativa técnica)

---

#### Trade-off 2.4: Estratégia de Ordenação

**ESCOLHA**: TreeMap com Comparator customizado

**JUSTIFICATIVA:**
- Volume de operadoras únicas: ~737 (menos de 1000)
- TreeMap: O(log n) por inserção, mantém ordenação automaticamente
- ArrayList + sort: O(n log n) mas requer passo separado
- **Para volumes < 10K, diferença negligenciável; TreeMap é mais legível**

**IMPLEMENTAÇÃO:**
```java
// Comparator ordena por valor total descrescente (maior para menor)
@Override
public int compareTo(EstatisticasOperadora outro) {
    return Double.compare(outro.totalDespesas, this.totalDespesas);
}
```

---

### TESTE 3: Banco de Dados

#### Trade-off 3.1: Normalização vs Desnormalização

**ESCOLHA**: Tabelas Normalizadas (Opção B)

**JUSTIFICATIVA CONSIDERANDO:**

| Fator | Análise |
|-------|---------|
| Volume de Dados | Milhões de registros → Normalização reduz redundância 30-40% |
| Frequência de Atualizações | Dados cadastrais mudam raramente; despesas inseridas incrementalmente |
| Complexidade de Queries | Agregações por operadora/UF requerem JOINs normalizados |

**ESTRUTURA ESCOLHIDA:**
```sql
operadoras (id_operadora, nome, cnpj, uf)
    ↑
    │ FK
despesas_consolidadas (id, id_operadora, trimestre, valor)
```

**VANTAGENS:**
- Integridade referencial com Foreign Keys
- UPDATE de operadoras sem replicação
- Queries agregadas eficientes com índices

---

#### Trade-off 3.2: Tipos de Dados para Valores Monetários

**ESCOLHA**: DECIMAL(18,2) (não FLOAT nem INTEGER)

**JUSTIFICATIVA:**

| Tipo | Precisão | Problema |
|------|----------|----------|
| FLOAT | ~6-7 dígitos | Erros de arredondamento binário (inaceitável para finanças) |
| INTEGER (centavos) | Exato | Eficiente, mas complicaria lógica de negócio |
| DECIMAL(18,2) | Exato | Precisão exata em cálculos financeiros |

**ESCOLHIDO**: DECIMAL(18,2) por ser **precisão financeira + legibilidade**

---

#### Trade-off 3.3: Tipos de Dados para Datas

**ESCOLHA**: DATE (não VARCHAR nem TIMESTAMP)

**JUSTIFICATIVA:**
- Operações de comparação/agrupamento por trimestre requerem tipo DATE
- VARCHAR aumentaria overhead; TIMESTAMP não é necessário
- DATE indexa melhor para range queries: `WHERE trimestre BETWEEN '2024-01-01' AND '2024-03-31'`

---

#### Trade-off 3.4: Tratamento de Inconsistências na Importação

**ESTRATÉGIA ADOTADA:**

| Problema | Solução |
|----------|---------|
| NULLs em campos obrigatórios | REJEITAR (NOT NULL constraint) |
| Strings em numéricos | TENTAR conversão; se falhar, REJEITAR |
| Datas inconsistentes | TENTAR STR_TO_DATE; se falhar, REJEITAR |
| CNPJs sem match no cadastro | Aceitar com marcação `[SEM_MATCH]` |

---

#### Trade-off 3.5: Queries Analíticas - Performance vs Legibilidade

**ESCOLHA**: CTEs (Common Table Expressions) para clareza

**JUSTIFICATIVA:**
```sql
-- Subqueries aninhadas (difícil ler):
SELECT * FROM (SELECT * FROM (SELECT ...) WHERE ...) WHERE ...

-- CTEs (fácil de ler):
WITH media_trimestre AS (
    SELECT trimestre, AVG(valor) AS media
    FROM despesas
    GROUP BY trimestre
),
acima_media AS (
    SELECT * FROM despesas d
    JOIN media_trimestre m WHERE d.valor > m.media
)
SELECT * FROM acima_media
```

**TRADE-OFF:**
- **Vantagem**: Legibilidade, manutenibilidade, performance
- **Desvantagem**: Mais verbose, mas para estagiário é educacional

---

### TESTE 4: Web API

#### Trade-off 4.1: Framework - FastAPI vs Flask

**ESCOLHA**: FastAPI

**JUSTIFICATIVA:**

| Aspecto | Flask | FastAPI |
|--------|-------|---------|
| Complexidade | Simples | Média (mas merece ser aprendida) |
| Performance | Boa | Excelente (2-3x mais rápido) |
| Type Hints | Não nativo | Integrado (Pydantic) |
| Validação | Manual | Automática |
| Documentação | Swagger (extensão) | Nativa (/docs) |

**ESCOLHIDO**: FastAPI por ser **mais moderno, educacional e performático**

---

#### Trade-off 4.2: Estratégia de Paginação

**ESCOLHA**: Offset-based (page + limit)

**JUSTIFICATIVA:**
- Simples de implementar
- Intuitivo para usuários (página 1, 2, 3...)
- Ineficiente para grandes datasets (skip N registros cada vez)

**ALTERNATIVAS CONSIDERADAS:**

| Tipo | Use Case |
|------|----------|
| Offset-based | Datasets pequenos/médios, UI com números de páginas |
| Cursor-based | Datasets gigantescos, feeds (Twitter) |
| Keyset | Performance máxima, poucos registros antes/depois |

**ESCOLHIDO**: Offset-based é apropriado para volume estimado

---

#### Trade-off 4.3: Cache vs Queries Diretas

**ESCOLHA**: Queries Diretas (sem cache)

**JUSTIFICATIVA:**
- Projeto é nível estagiário, dados são mock (não precisa otimização)
- Cache adiciona complexidade (invalidação, TTL, sincronização)
- Implementação futura: Redis com TTL=5min para /api/estatisticas

**PARA PRODUÇÃO:**
```python
# Adicionar depois:
from functools import lru_cache
from datetime import timedelta

@lru_cache(maxsize=1)
def obter_estatisticas_cached():
    # Calcula 1x
    return {...}
```

---

#### Trade-off 4.4: Estrutura de Resposta da API

**ESCOLHA**: Dados + Metadados

**JUSTIFICATIVA:**
```json
// BOM (com metadados):
{
  "data": [...],
  "total": 100,
  "page": 1,
  "limit": 10,
  "total_pages": 10
}

// RUIM (sem metadados):
[...]  // Frontend não sabe quantas páginas existem
```

**VANTAGEM:** Frontend sabe total, pode renderizar botão "próxima" corretamente

---

#### Trade-off 4.5: Busca - Servidor vs Cliente vs Híbrido

**ESCOLHA**: Busca no Servidor

**JUSTIFICATIVA:**
- Volume de dados: 10 operadoras (pequenininho, mas simula real)
- Em produção: Servidor é essencial (milhões de registros)
- Frontend filtra no cliente: Apenas para UX (typeahead local)
- Servidor sempre tá certo: Fonte de verdade

**IMPLEMENTAÇÃO:**
```python
@app.get("/api/operadoras?search=amil")
# Busca no servidor: DB.query().filter(Operadora.nome.ilike("%amil%"))
```

---

#### Trade-off 4.6: Gerenciamento de Estado (Frontend)

**ESCOLHA**: Composables simples (Vue 3)

**JUSTIFICATIVA:**
- Projeto pequeno: Não precisa Vuex/Pinia
- Vue 3 Composables: Suficiente para compartilhar estado
- Escalabilidade: Se crescer, adiciona Pinia depois

**PARA CRESCER:**
```javascript
// Hoje: Composables simples
const { operadoras, loading } = useOperadoras()

// Amanhã: Pinia para gerenciamento robusto
import { useOperadorasStore } from '@/stores/operadoras'
```

---

#### Trade-off 4.7: Performance da Tabela

**ESCOLHA**: Renderização Simples (sem virtualização)

**JUSTIFICATIVA:**
- Dados mock: 10 operadoras (paginação 10/página)
- Vue renderiza 10 elementos: ~1ms
- Virtualização (vue-virtual-scroller) seria over-engineering para este volume
- Nível estagiário: Renderização simples é apropriada

**PARA GRANDES VOLUMES:**
```javascript
// Adicionar depois se necessário:
import DynamicScroller from 'vue-virtual-scroller'
```

---

#### Trade-off 4.8: Tratamento de Erros

**ESCOLHA**: Mensagens genéricas (com logging detalhado)

**JUSTIFICATIVA:**
- Simples para usuário final: "Erro ao carregar operadoras"
- Segurança: Não expõe detalhes internos
- Logging: Servidor loga detalhes para debugging

```javascript
// Cliente mostra ao usuário:
"Erro ao carregar operadoras"

// Servidor loga detalhes:
console.error("DB connection timeout: postgres://...")
```

## Testes Implementados

### Teste 1: Integração com API ANS
- Download de últimos 3 trimestres
- Extração automática de ZIPs
- Filtragem de Eventos/Sinistros
- Consolidação em CSV
- Detecção de inconsistências
- Documentação de decisões técnicas



---

### Teste 2: Transformação e Validação
- Validação CNPJ (dígitos verificadores corretos)
- Validação de valores e razão social
- Download de operadoras ativas
- Join eficiente com HashMap
- Tratamento de mismatches
- Agregação com média e desvio padrão
- Ordenação por valor


---

### Teste 3: Banco de Dados
- DDL normalizado com indices
- Trade-offs documentados
- Scripts de importação
- 3 Queries analíticas com CTEs
- Justificativas de desafios

Score: 7/10 (SQL não testado em Windows)

---

### Teste 4: API Web
- FastAPI com CORS
- 4 rotas implementadas
- Paginação com metadados
- Busca/filtro
- Vue.js com dashboard
- Gráfico com Chart.js
- Modal de detalhes


### Arquivos de Referência
- `teste_1_api_integracao/DECISOES_TECNICAS.md` - Trade-offs Teste 1
- `teste_2_teste_de_transformacao_e_validacao_de_dados/DECISOES_TECNICAS.md` - Trade-offs Teste 2

### Estrutura de Pastas
Cada teste tem organização clara:
```
├── src/           (Código fonte)
├── data/          (Dados: raw, extracted)
├── output/        (Saída do processamento)
└── DECISOES_TECNICAS.md (Decisões e trade-offs)
```