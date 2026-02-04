# Teste 4: API e Interface Web - Nível Estagiário

Projeto simples com backend FastAPI e frontend Vue.js 3 para análise de despesas de operadoras.

## Como Rodar

### Backend

```bash
cd backend
pip install -r requirements.txt
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

API em: http://localhost:8000
Docs: http://localhost:8000/docs

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Interface em: http://localhost:5173

## Rotas da API

- `GET /api/operadoras` - Lista operadoras (com paginação)
- `GET /api/operadoras/{cnpj}` - Detalhes de uma operadora
- `GET /api/operadoras/{cnpj}/despesas` - Despesas de uma operadora
- `GET /api/estatisticas` - Estatísticas agregadas
- `GET /health` - Health check

## Funcionalidades

✅ Dashboard com 3 cards (total, média, mediana)
✅ Top 5 operadoras por despesa
✅ Tabela de operadoras com busca
✅ Paginação de resultados
✅ Modal com detalhes da operadora

## Stack

- Backend: FastAPI + Pydantic + Uvicorn
- Frontend: Vue.js 3 + Vite + Tailwind CSS
- Dados: Mock em memória (10 operadoras, 40 despesas)
```bash
cd frontend
npm install
```

#### 2. Executar dev server
```bash
npm run dev
```

Acesse: http://localhost:5173

#### 3. Build para produção
```bash
npm run build
```

---

## 📚 Rotas da API

### 1. **GET /api/operadoras**
Lista operadoras com paginação.

**Parâmetros:**
- `page` (int, default=1): Número da página
- `limit` (int, default=10): Itens por página (máx 100)
- `search` (string, opcional): Buscar por nome ou CNPJ

**Resposta:**
```jsongit add .
git commit -m "feat: implementar pipeline completo de análise de despesas de operadoras

BREAKING CHANGE: Primeira versão do projeto com todos os 4 testes integrados

Implementa solução end-to-end para análise de despesas de operadoras de saúde:

Teste 1 - Integração com API ANS:
- Download automático de últimos 3 trimestres
- Extração e processamento de ZIPs
- Consolidação de dados Eventos/Sinistros em CSV
- Detecção e logging de inconsistências

Teste 2 - Transformação e Validação:
- Validação de CNPJs com dígitos verificadores
- Enriquecimento de dados com cadastro de operadoras
- Agregação por UF com cálculo de média e desvio padrão
- Tratamento robusto de mismatches

Teste 3 - Banco de Dados:
- Schema normalizado com 3 tabelas
- Scripts SQL com DDL, importação e 3 queries analíticas com CTEs
- Índices otimizados para performance
- Suporte a re-execução com IF NOT EXISTS

Teste 4 - API Web:
- FastAPI backend com 4 rotas
- Frontend Vue.js 3 com dashboard interativo
- Busca e filtros com Chart.js

Documentação:
- README.md com trade-offs técnicos (8 seções)
- Instruções de execução para cada teste

Tecnologias: Java 11, Python 3.9, MySQL 8.0, Node.js 16+, Vue.js 3, FastAPI"

git push origin main
{
  "data": [...],
  "total": 100,
  "page": 1,
  "limit": 10,
  "total_pages": 10
}
```

**Exemplo:**
```bash
curl "http://localhost:8000/api/operadoras?page=1&limit=10"
```

---

### 2. **GET /api/operadoras/{cnpj}**
Detalhes de uma operadora específica.

**Parâmetros:**
- `cnpj` (string): CNPJ com ou sem formatação

**Resposta:**
```json
{
  "id_operadora": 1,
  "nome": "Amil Assistência Médica Internacional",
  "cnpj": "17.197.385/0001-21",
  "uf": "SP",
  "tipo": "Medicina de Grupo"
}
```

**Exemplo:**
```bash
curl "http://localhost:8000/api/operadoras/17.197.385/0001-21"
```

---

### 3. **GET /api/operadoras/{cnpj}/despesas**
Histórico de despesas de uma operadora.

**Resposta:**
```json
[
  {
    "id": 1,
    "id_operadora": 1,
    "trimestre": "2024-01-01",
    "valor_despesa": 2500000.50
  }
]
```

**Exemplo:**
```bash
curl "http://localhost:8000/api/operadoras/17.197.385/0001-21/despesas"
```

---

### 4. **GET /api/estatisticas**
Estatísticas agregadas (com cache de 5 minutos).

**Resposta:**
```json
{
  "total_despesas": 125000000.50,
  "media_despesas": 3125000.12,
  "mediana_despesas": 2875000.00,
  "top_5_operadoras": [...],
  "distribuicao_por_uf": {...},
  "timestamp": "2024-02-03T10:40:25.789456"
}
```

**Exemplo:**
```bash
curl "http://localhost:8000/api/estatisticas"
```

---

## 🎨 Recursos da Interface

### Dashboard
- 📊 Estatísticas principais (total, média, mediana)
- 📈 Gráfico de distribuição por UF (Chart.js)
- 🏆 Top 5 operadoras por despesa

### Operadoras
- 📋 Tabela paginada com todas as operadoras
- 🔍 Busca por nome/CNPJ (híbrido: local + servidor)
- 👁️ Modal com detalhes e histórico de despesas
- ⚙️ Tratamento de loading, erros e empty states

---

## 🧪 Testando com Postman

1. **Importar coleção:**
   - Abra Postman
   - `File → Import`
   - Selecione `Intuitive_Care_API.postman_collection.json`

2. **Rodar requisições:**
   - Certifique-se que a API está rodando (http://localhost:8000)
   - Clique em qualquer rota para executar

3. **Exemplos pré-configurados:**
   - Listar operadoras
   - Buscar por nome
   - Obter detalhes
   - Carregar despesas
   - Visualizar estatísticas

---

## 🔧 Trade-offs Técnicos

Consulte `DECISOES_TECNICAS.md` para análise detalhada:

### Backend
- ✅ **Framework:** FastAPI (performance, type hints, docs automáticas)
- ✅ **Paginação:** Offset-based (simples, adequada p/ volume)
- ✅ **Cache:** 5 minutos para estatísticas (balance performance/freshness)
- ✅ **Resposta:** Dados + Metadados (melhor UX)

### Frontend
- ✅ **Busca:** Híbrida (local + debounce para servidor)
- ✅ **Estado:** Composition API (simples, sem boilerplate)
- ✅ **Performance:** Paginação (não precisa virtual scrolling)
- ✅ **Erros:** Estados explícitos (loading, erro, vazio)

---

## 📝 Dados Mock

A API utiliza dados mock em memória:
- **10 operadoras** (Amil, Bradesco, Unimed, etc.)
- **40 registros de despesas** (4 trimestres × 10 operadoras)
- **Estados:** SP, RJ, MG, CE, DF

Para conectar a um banco de dados real, modifique:
```python
# backend/app/main.py
# Substitua _load_operadoras() e _load_despesas() com:

def _load_operadoras():
    # from sqlalchemy import create_engine
    # session = create_engine(...).connect()
    # return session.query(Operadora).all()
```

---

## 🌐 Deployment

### Docker
```bash
docker-compose up
```

### Variáveis de Ambiente
```bash
# .env
DATABASE_URL=postgresql://user:pass@localhost:5432/intuitive_care
REDIS_URL=redis://localhost:6379
API_CORS_ORIGINS=http://localhost:3000,https://app.example.com
CACHE_TTL=300
```

---

## 📊 Monitoramento

### Backend (FastAPI)
```bash
# Auto-generated docs
http://localhost:8000/docs        # Swagger UI
http://localhost:8000/redoc       # ReDoc
```

### Frontend (Vite)
```bash
# Performance
npm run build --profile          # Analisa tamanho
http://localhost:5173/__vite_ping # Health check
```

---

## 🐛 Troubleshooting

### CORS Error
```
// frontend recebe erro de CORS
✅ Solução: Certifique-se que backend está rodando e aceitando origins
```

### API não responde
```bash
# Verificar se backend está online
curl http://localhost:8000/health

# Se 200 OK - backend está respondendo
# Se erro - iniciar backend
```

### Dados vazios
```
# Dados são mock em memória - sempre mesmos valores
# Para dados dinâmicos: conectar a banco real (ver seção anterior)
```

---

## 📚 Documentação Adicional

- `DECISOES_TECNICAS.md` - Trade-offs técnicos detalhados
- `Intuitive_Care_API.postman_collection.json` - Exemplos de requisições
- Comentários inline no código explicando implementações

---

## 📄 Licença

MIT

---

## ✅ Checklist de Implementação

- [x] Backend FastAPI com 4 rotas
- [x] Paginação offset-based
- [x] Cache de estatísticas (5 min)
- [x] Resposta estruturada com metadados
- [x] Frontend Vue.js 3 com Composition API
- [x] Tabela paginada com operadoras
- [x] Busca/filtro híbrido
- [x] Gráfico com Chart.js
- [x] Modal com detalhes e histórico
- [x] Tratamento de estados (loading/erro/vazio)
- [x] Coleção Postman
- [x] Documentação de trade-offs
- [x] README com instruções

**Status:** ✅ COMPLETO
