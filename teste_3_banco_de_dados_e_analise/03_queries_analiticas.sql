-- TESTE 3: BANCO DE DADOS E ANÁLISE
-- Queries analíticas

-- Query 1: 5 operadoras com maior crescimento percentual de despesas entre o primeiro e o último trimestre
SELECT
    o.nome,
    ((ult.valor_despesa - prim.valor_despesa) / prim.valor_despesa) * 100 AS crescimento_percentual
FROM
    operadoras o
    JOIN (
        SELECT id_operadora, MIN(trimestre) AS primeiro, MAX(trimestre) AS ultimo
        FROM despesas_consolidadas
        GROUP BY id_operadora
    ) t ON o.id_operadora = t.id_operadora
    JOIN despesas_consolidadas prim ON prim.id_operadora = t.id_operadora AND prim.trimestre = t.primeiro
    JOIN despesas_consolidadas ult ON ult.id_operadora = t.id_operadora AND ult.trimestre = t.ultimo
WHERE prim.valor_despesa > 0
ORDER BY crescimento_percentual DESC
LIMIT 5;
-- Justificativa: Operadoras sem dados em todos os trimestres são excluídas para garantir cálculo correto.

-- Query 2: Distribuição de despesas por UF (top 5 estados) e média por operadora
-- Desafio adicional: Calcular média de despesas POR OPERADORA em cada UF (não apenas total)
SELECT
    o.uf,
    SUM(dc.valor_despesa) AS total_despesas_uf,
    COUNT(DISTINCT dc.id_operadora) AS qtd_operadoras,
    SUM(dc.valor_despesa) / COUNT(DISTINCT dc.id_operadora) AS media_por_operadora_uf,
    ROUND(AVG(dc.valor_despesa), 2) AS media_por_registro
FROM
    despesas_consolidadas dc
    JOIN operadoras o ON dc.id_operadora = o.id_operadora
GROUP BY o.uf
ORDER BY total_despesas_uf DESC
LIMIT 5;
-- Justificativa: 
--   - SUM(valor_despesa) / COUNT(DISTINCT id_operadora) = média por operadora
--   - Exemplo: UF 'SP' com 3 operadoras e despesas totais de 900 = média 300 por operadora
--   - COUNT(DISTINCT id_operadora) evita contar mesma operadora múltiplas vezes

-- Query 3: Operadoras com despesas acima da média geral em pelo menos 2 dos 3 trimestres
WITH media_trimestre AS (
    SELECT trimestre, AVG(valor_despesa) AS media
    FROM despesas_consolidadas
    GROUP BY trimestre
),
acima_media AS (
    SELECT dc.id_operadora, dc.trimestre
    FROM despesas_consolidadas dc
    JOIN media_trimestre mt ON dc.trimestre = mt.trimestre
    WHERE dc.valor_despesa > mt.media
)
SELECT
    o.nome,
    COUNT(DISTINCT am.trimestre) AS trimestres_acima_media
FROM
    acima_media am
    JOIN operadoras o ON am.id_operadora = o.id_operadora
GROUP BY am.id_operadora, o.nome
HAVING COUNT(DISTINCT am.trimestre) >= 2;
-- Justificativa: Uso de CTEs para clareza, performance e manutenibilidade.
