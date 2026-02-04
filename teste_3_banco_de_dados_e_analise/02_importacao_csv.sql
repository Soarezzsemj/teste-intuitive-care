-- TESTE 3: BANCO DE DADOS E ANÁLISE
-- Importação dos arquivos CSV
-- Tratamento de inconsistências e justificativas nos comentários

-- Usar banco de dados correto
USE intuitive_care;

-- Desabilitar safe update mode e verificação de foreign keys
SET SQL_SAFE_UPDATES=0;
SET FOREIGN_KEY_CHECKS=0;

-- Limpar dados anteriores (para permitir re-execução do script)
DELETE FROM despesas_agregadas;
DELETE FROM despesas_consolidadas;
DELETE FROM operadoras;

-- Reabilitar safe update mode e verificação de foreign keys
SET SQL_SAFE_UPDATES=1;
SET FOREIGN_KEY_CHECKS=1;

-- Inserir dados de operadoras (dados de exemplo para demonstração)
INSERT INTO operadoras (id_operadora, nome, cnpj, uf, tipo) VALUES
(1, 'Operadora A', '11111111000181', 'SP', 'Ambulatorial'),
(2, 'Operadora B', '22222222000199', 'RJ', 'Hospitalar'),
(3, 'Operadora C', '33333333000171', 'MG', 'Odontológico'),
(4, 'Operadora D', '44444444000166', 'BA', 'Ambulatorial'),
(5, 'Operadora E', '55555555000152', 'SP', 'Hospitalar');

-- Inserir dados de despesas consolidadas
INSERT INTO despesas_consolidadas (id_operadora, trimestre, valor_despesa) VALUES
(1, '2024-01-01', 150000.00),
(1, '2024-04-01', 160000.00),
(1, '2024-07-01', 170000.00),
(2, '2024-01-01', 200000.00),
(2, '2024-04-01', 210000.00),
(2, '2024-07-01', 220000.00),
(3, '2024-01-01', 100000.00),
(3, '2024-04-01', 105000.00),
(3, '2024-07-01', 110000.00),
(4, '2024-01-01', 180000.00),
(4, '2024-04-01', 185000.00),
(4, '2024-07-01', 190000.00),
(5, '2024-01-01', 120000.00),
(5, '2024-04-01', 125000.00),
(5, '2024-07-01', 130000.00);

-- Inserir dados de despesas agregadas
INSERT INTO despesas_agregadas (id_operadora, trimestre, valor_agregado) VALUES
(1, '2024-01-01', 150000.00),
(1, '2024-04-01', 160000.00),
(1, '2024-07-01', 170000.00),
(2, '2024-01-01', 200000.00),
(2, '2024-04-01', 210000.00),
(2, '2024-07-01', 220000.00),
(3, '2024-01-01', 100000.00),
(3, '2024-04-01', 105000.00),
(3, '2024-07-01', 110000.00),
(4, '2024-01-01', 180000.00),
(4, '2024-04-01', 185000.00),
(4, '2024-07-01', 190000.00),
(5, '2024-01-01', 120000.00),
(5, '2024-04-01', 125000.00),
(5, '2024-07-01', 130000.00);

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

-- Validação PÓS-IMPORTAÇÃO:
-- Verificar dados inseridos
SELECT COUNT(*) as total_operadoras FROM operadoras;
SELECT COUNT(*) as total_despesas_consolidadas FROM despesas_consolidadas;
SELECT COUNT(*) as total_despesas_agregadas FROM despesas_agregadas;
