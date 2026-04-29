-- Criação dos tipos ENUM mapeados nas queries nativas do Repository
CREATE TYPE tipolote AS ENUM ('IMOVEL_RESIDENCIAL', 'IMOVEL_COMERCIAL', 'IMOVEL_RURAL', 'VEICULO', 'MAQUINARIO', 'OUTROS');
CREATE TYPE origemleilao AS ENUM ('JUDICIAL', 'EXTRAJUDICIAL');
CREATE TYPE statuslote AS ENUM ('DISPONIVEL', 'ENCERRADO', 'CANCELADO', 'ARREMATADO');

-- Tabela principal
CREATE TABLE lotes (
    id BIGSERIAL PRIMARY KEY,
    fonte VARCHAR(50) NOT NULL,
    id_externo VARCHAR(100) NOT NULL,
    url_original TEXT NOT NULL,
    
    tipo tipolote NOT NULL DEFAULT 'OUTROS',
    origem origemleilao,
    status statuslote NOT NULL DEFAULT 'DISPONIVEL',
    
    valor_avaliacao NUMERIC(15, 2),
    valor_lance_inicial NUMERIC(15, 2) NOT NULL,
    valor_incremento NUMERIC(15, 2),
    
    aceita_financiamento BOOLEAN NOT NULL DEFAULT FALSE,
    aceita_fgts BOOLEAN NOT NULL DEFAULT FALSE,
    banco_financiador VARCHAR(100),
    
    logradouro VARCHAR(255),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    estado VARCHAR(2),
    cep VARCHAR(9),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    
    descricao TEXT,
    area_m2 NUMERIC(10, 2),
    matricula VARCHAR(50),
    debitos_conhecidos TEXT,
    ocupado BOOLEAN,
    
    data_leilao TIMESTAMP,
    data_primeiro_leilao TIMESTAMP,
    data_segundo_leilao TIMESTAMP,
    
    score_oportunidade NUMERIC(5, 2),
    score_desconto NUMERIC(5, 2),
    score_financiamento NUMERIC(5, 2),
    score_localizacao NUMERIC(5, 2),
    score_risco NUMERIC(5, 2),
    score_calculado_em TIMESTAMP,
    
    coletado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP,
    fonte_scraper_versao VARCHAR(20),
    html_raw TEXT,
    
    CONSTRAINT uq_lotes_fonte_id_externo UNIQUE (fonte, id_externo)
);

-- Índices essenciais e mencionados no modelo
CREATE INDEX ix_lotes_data_leilao ON lotes (data_leilao);
CREATE INDEX ix_lotes_cidade_estado ON lotes (cidade, estado);
CREATE INDEX ix_lotes_tipo_status ON lotes (tipo, status);
CREATE INDEX ix_lotes_score ON lotes (score_oportunidade);