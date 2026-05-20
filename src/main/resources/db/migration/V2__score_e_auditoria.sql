-- V2__score_e_auditoria.sql
-- Adiciona índice para score_calculado_em e coluna de versão do scraper.
-- Equivalente à migration 0002_score_e_auditoria.py do Alembic.
-- (As colunas de score já foram criadas na V1 — esta migration adiciona só o índice)

CREATE INDEX ix_lotes_score_calculado_em ON lotes (score_calculado_em);
