-- docker/init-db.sql
-- Executado pelo PostgreSQL na primeira inicialização do container.
-- O Flyway cuida das tabelas — este script só configura extensões e permissões.

-- Extensão para UUIDs (útil para chaves futuras)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Fuso horário do banco alinhado com Brasil (UTC-3)
ALTER DATABASE leiloes SET timezone TO 'America/Sao_Paulo';

-- Log de criação
DO $$
BEGIN
  RAISE NOTICE 'Banco leiloes inicializado com sucesso.';
END;
$$;
