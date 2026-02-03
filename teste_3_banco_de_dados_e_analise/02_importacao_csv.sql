-- TESTE 3: BANCO DE DADOS E ANÁLISE
-- Importação dos arquivos CSV
-- Tratamento de inconsistências e justificativas nos comentários

-- Importar operadoras
LOAD DATA INFILE '/caminho/operadoras.csv'
INTO TABLE operadoras
CHARACTER SET utf8
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(id_operadora, nome, cnpj, uf, tipo);

-- Importar despesas consolidadas
LOAD DATA INFILE '/caminho/consolidado_despesas.csv'
INTO TABLE despesas_consolidadas
CHARACTER SET utf8
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(id_operadora, trimestre, valor_despesa);

-- Importar despesas agregadas
LOAD DATA INFILE '/caminho/despesas_agregadas.csv'
INTO TABLE despesas_agregadas
CHARACTER SET utf8
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(id_operadora, trimestre, valor_agregado);

-- TRATAMENTO DE INCONSISTÊNCIAS DURANTE IMPORTAÇÃO:

-- Estratégia de validação em 3 etapas:
-- 1. PRÉ-IMPORTAÇÃO: Validar integridade dos dados CSV
-- 2. IMPORTAÇÃO: Usar triggers/constraints para rejeitar dados inválidos
-- 3. PÓS-IMPORTAÇÃO: Registrar erros em tabela de auditoria

-- Casos específicos de tratamento:
-- a) Valores NULL em campos obrigatórios:
--    Abordagem: REJEITAR e registrar na tabela erro_importacao
--    Justificativa: Integridade referencial; NULL em id_operadora quebraria FK
--    Exemplo: id_operadora, trimestre, valor_despesa são NOT NULL

-- b) Strings em campos numéricos (ex: "abc" em valor_despesa):
--    Abordagem: TENTAR conversão CAST; se falhar, REJEITAR
--    Justificativa: Pode haver espaços/formatação; conversão recupera alguns dados
--    Limite: Após 3 tentativas de conversão, desistir (dado inválido)

-- c) Datas em formatos inconsistentes (ex: "03/02/2024" vs "2024-02-03"):
--    Abordagem: TENTAR conversão para YYYY-MM-DD; se falhar, REJEITAR
--    Justificativa: Trimestre deve ser sempre primeiro dia (YYYY-01-01, YYYY-04-01, etc)
--    Exemplo: STR_TO_DATE('03/02/2024', '%d/%m/%Y')

-- d) Operadoras duplicadas ou não cadastradas:
--    Abordagem: Validar FK antes de INSERT; usar ON DUPLICATE KEY UPDATE se apropriado
--    Justificativa: Evita duplicação; garante integridade referencial

-- Tabela de auditoria para registrar erros:
CREATE TABLE IF NOT EXISTS erro_importacao (
    id INT PRIMARY KEY AUTO_INCREMENT,
    data_importacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    arquivo VARCHAR(100),
    linha_arquivo INT,
    tipo_erro VARCHAR(50),
    descricao TEXT,
    dados_rejeitados TEXT
);

-- Queries de validação PRÉ-IMPORTAÇÃO:
-- Verificar número de linhas e colunas esperadas
SELECT COUNT(*) as total_linhas FROM temp_operadoras;
SELECT COUNT(*) FROM temp_consolidado WHERE valor_despesa IS NULL;
SELECT COUNT(*) FROM temp_consolidado WHERE id_operadora NOT IN (SELECT id_operadora FROM operadoras);
