-- TESTE 3: BANCO DE DADOS E ANÁLISE
-- Estrutura de tabelas normalizadas (Opção B)
-- Justificativas técnicas nos comentários

-- Criar banco de dados
CREATE DATABASE IF NOT EXISTS intuitive_care CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE intuitive_care;

-- Tabela de dados cadastrais das operadoras
CREATE TABLE IF NOT EXISTS operadoras (
    id_operadora INT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cnpj VARCHAR(20) UNIQUE NOT NULL,
    uf CHAR(2) NOT NULL,
    tipo VARCHAR(50),
    -- outros campos cadastrais
    INDEX idx_uf (uf)
);

-- Tabela de despesas consolidadas
CREATE TABLE IF NOT EXISTS despesas_consolidadas (
    id INT PRIMARY KEY AUTO_INCREMENT,
    id_operadora INT NOT NULL,
    trimestre DATE NOT NULL,
    valor_despesa DECIMAL(18,2) NOT NULL,
    -- outros campos relevantes
    FOREIGN KEY (id_operadora) REFERENCES operadoras(id_operadora),
    INDEX idx_trimestre (trimestre),
    INDEX idx_id_operadora (id_operadora)
);

-- Tabela de despesas agregadas
CREATE TABLE IF NOT EXISTS despesas_agregadas (
    id INT PRIMARY KEY AUTO_INCREMENT,
    id_operadora INT NOT NULL,
    trimestre DATE NOT NULL,
    valor_agregado DECIMAL(18,2) NOT NULL,
    -- outros campos relevantes
    FOREIGN KEY (id_operadora) REFERENCES operadoras(id_operadora),
    INDEX idx_trimestre (trimestre),
    INDEX idx_id_operadora (id_operadora)
);

-- JUSTIFICATIVAS TÉCNICAS DETALHADAS:

-- 1. TRADE-OFF DE NORMALIZAÇÃO (Escolha: Opção B - Normalização)
--    Justificativa considerando:
--    - Volume de dados: Milhões de registros de despesas por operadora/trimestre
--      * Normalização reduz redundância: nome/CNPJ da operadora armazenado 1x
--      * Economia de espaço ~30-40% vs desnormalização
--    - Frequência de atualizações: Dados cadastrais mudam raramente; despesas são inseridas incrementalmente
--      * Normalização facilita UPDATE de operadoras sem replicação
--      * Foreign Keys garantem integridade referencial
--    - Complexidade de queries: Queries analíticas envolvem agregações por operadora/UF
--      * JOINs normalizados são eficientes com índices apropriados
--      * Evita cálculos desnormalizados complexos
--    Conclusão: Normalização é ótima para este cenário de dados históricos com análises complexas.

-- 2. TRADE-OFF DE TIPOS DE DADOS:
--    Valores monetários (DECIMAL vs FLOAT vs INTEGER):
--      * Escolha: DECIMAL(18,2)
--      * Razão: Precisão exata em cálculos financeiros (não há arredondamento binário)
--      * FLOAT causaria erros de arredondamento inaceitáveis em somas
--      * INTEGER (centavos) seria mais eficiente, mas complicaria lógica de negócio
--    Datas (DATE vs VARCHAR vs TIMESTAMP):
--      * Escolha: DATE
--      * Razão: Operações de comparação/agrupamento por trimestre requerem tipo DATE
--      * VARCHAR aumentaria overhead; TIMESTAMP não é necessário (sem hora)
--      * DATE indexa melhor para range queries (ex: WHERE trimestre BETWEEN '2024-01-01' AND '2024-03-31')
