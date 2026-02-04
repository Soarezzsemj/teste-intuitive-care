"""
TESTE 4: API E INTERFACE WEB - Nível Estagiário
Backend FastAPI simples com 4 rotas
"""

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
from datetime import datetime
import random

# ============================================================================
# INICIALIZAÇÃO
# ============================================================================
app = FastAPI(title="API Intuitive Care", version="1.0.0")

# CORS - permitir localhost
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ============================================================================
# MODELOS
# ============================================================================
class Operadora(BaseModel):
    id_operadora: int
    nome: str
    cnpj: str
    uf: str
    tipo: Optional[str] = None

class Despesa(BaseModel):
    id: int
    id_operadora: int
    trimestre: str
    valor_despesa: float

# ============================================================================
# DADOS (em memória)
# ============================================================================

# 10 Operadoras
OPERADORAS = [
    Operadora(id_operadora=1, nome="Amil", cnpj="17.197.385/0001-21", uf="SP", tipo="Medicina"),
    Operadora(id_operadora=2, nome="Bradesco", cnpj="07.455.999/0001-80", uf="RJ", tipo="Seguro"),
    Operadora(id_operadora=3, nome="SulAmérica", cnpj="35.188.869/0001-70", uf="MG", tipo="Seguro"),
    Operadora(id_operadora=4, nome="Unimed", cnpj="41.672.190/0001-70", uf="SP", tipo="Cooperativa"),
    Operadora(id_operadora=5, nome="Caixa", cnpj="36.402.401/0001-95", uf="DF", tipo="Seguro"),
    Operadora(id_operadora=6, nome="Notre Dame", cnpj="17.098.066/0001-84", uf="RJ", tipo="Medicina"),
    Operadora(id_operadora=7, nome="Geap", cnpj="29.140.261/0001-22", uf="SP", tipo="Associação"),
    Operadora(id_operadora=8, nome="Hapvida", cnpj="06.171.633/0001-01", uf="CE", tipo="Medicina"),
    Operadora(id_operadora=9, nome="Itaú", cnpj="17.197.092/0001-21", uf="SP", tipo="Seguro"),
    Operadora(id_operadora=10, nome="Mediservice", cnpj="01.614.188/0001-36", uf="RJ", tipo="Medicina"),
]

# 40 Despesas (4 trimestres x 10 operadoras)
DESPESAS = []
id_desp = 1
trimestres = ["2024-01-01", "2024-04-01", "2024-07-01", "2024-10-01"]
for id_op in range(1, 11):
    for trimestre in trimestres:
        DESPESAS.append(Despesa(
            id=id_desp,
            id_operadora=id_op,
            trimestre=trimestre,
            valor_despesa=round(random.uniform(1000000, 5000000), 2)
        ))
        id_desp += 1

# ============================================================================
# ROTAS
# ============================================================================

@app.get("/api/operadoras")
async def listar_operadoras(page: int = 1, limit: int = 10, search: Optional[str] = None):
    """Lista operadoras com paginação e busca"""
    operadoras = OPERADORAS
    
    # Buscar
    if search:
        search = search.lower()
        operadoras = [op for op in operadoras if search in op.nome.lower() or search in op.cnpj]
    
    # Paginação
    total = len(operadoras)
    offset = (page - 1) * limit
    dados = operadoras[offset:offset + limit]
    
    return {
        "data": [op.dict() for op in dados],
        "total": total,
        "page": page,
        "limit": limit,
        "total_pages": (total + limit - 1) // limit
    }

@app.get("/api/operadoras/{cnpj}")
async def obter_operadora(cnpj: str):
    """Detalhes de uma operadora"""
    cnpj_clean = cnpj.replace(".", "").replace("/", "").replace("-", "")
    
    for op in OPERADORAS:
        op_cnpj_clean = op.cnpj.replace(".", "").replace("/", "").replace("-", "")
        if op_cnpj_clean == cnpj_clean:
            return op.dict()
    
    raise HTTPException(status_code=404, detail="Operadora não encontrada")

@app.get("/api/operadoras/{cnpj}/despesas")
async def obter_despesas(cnpj: str):
    """Despesas de uma operadora"""
    cnpj_clean = cnpj.replace(".", "").replace("/", "").replace("-", "")
    
    # Encontrar operadora
    operadora = None
    for op in OPERADORAS:
        op_cnpj_clean = op.cnpj.replace(".", "").replace("/", "").replace("-", "")
        if op_cnpj_clean == cnpj_clean:
            operadora = op
            break
    
    if not operadora:
        raise HTTPException(status_code=404, detail="Operadora não encontrada")
    
    # Despesas da operadora
    despesas = [d for d in DESPESAS if d.id_operadora == operadora.id_operadora]
    return [d.dict() for d in despesas]

@app.get("/api/estatisticas")
async def obter_estatisticas():
    """Estatísticas agregadas"""
    valores = [d.valor_despesa for d in DESPESAS]
    
    # Total e média
    total = sum(valores)
    media = total / len(valores) if valores else 0
    
    # Mediana
    mediana = sorted(valores)[len(valores) // 2] if valores else 0
    
    # Top 5 operadoras
    por_operadora = {}
    for d in DESPESAS:
        por_operadora[d.id_operadora] = por_operadora.get(d.id_operadora, 0) + d.valor_despesa
    
    top_5 = sorted(por_operadora.items(), key=lambda x: x[1], reverse=True)[:5]
    top_5_dados = []
    for id_op, total_op in top_5:
        op = next(o for o in OPERADORAS if o.id_operadora == id_op)
        top_5_dados.append({
            "id_operadora": id_op,
            "nome": op.nome,
            "total_despesas": total_op,
            "uf": op.uf
        })
    
    # Distribuição por UF
    por_uf = {}
    for d in DESPESAS:
        op = next(o for o in OPERADORAS if o.id_operadora == d.id_operadora)
        por_uf[op.uf] = por_uf.get(op.uf, 0) + d.valor_despesa
    
    return {
        "total_despesas": total,
        "media_despesas": media,
        "mediana_despesas": mediana,
        "top_5_operadoras": top_5_dados,
        "distribuicao_por_uf": por_uf,
        "timestamp": datetime.now().isoformat()
    }

@app.get("/health")
async def health():
    """Health check"""
    return {"status": "ok"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
