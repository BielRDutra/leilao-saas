-- Índice para buscar rapidamente lotes sem score ou para checar quando foram calculados
CREATE INDEX ix_lotes_score_calculado_em ON lotes (score_calculado_em);