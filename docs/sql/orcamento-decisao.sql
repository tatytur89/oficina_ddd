-- Atualizacao incremental para bancos PostgreSQL existentes.
-- Aplicar antes do rollout que usa versao/decisao do orcamento com ddl-auto=validate.
-- Este script nao cria o schema inicial nem substitui o provisionamento do banco.
BEGIN;

ALTER TABLE ordens_servico
    ADD COLUMN IF NOT EXISTS versao_orcamento BIGINT DEFAULT 0;

ALTER TABLE ordens_servico
    ADD COLUMN IF NOT EXISTS decisao_orcamento BOOLEAN;

COMMIT;
