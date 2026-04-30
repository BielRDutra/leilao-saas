-- V3__cria_tabela_assinantes_e_historico.sql
-- Cria as tabelas do sistema de alertas.

-- ── Assinantes ─────────────────────────────────────────────────────────────
-- Cada assinante define seus critérios de alerta.
-- Um mesmo usuário pode ter múltiplos perfis (ex: SP score≥70, RJ score≥60).
CREATE TABLE assinantes (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(100),
    email           VARCHAR(255),   -- null = não recebe e-mail
    whatsapp        VARCHAR(20),    -- null = não recebe WhatsApp (ex: "5511999990000")
    ativo           BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Critérios do alerta
    score_minimo    NUMERIC(5,2)    NOT NULL DEFAULT 50.0,
    cidade          VARCHAR(100),   -- null = qualquer cidade
    estado          VARCHAR(2),     -- null = qualquer estado
    tipo_lote       VARCHAR(30),    -- null = qualquer tipo

    criado_em       TIMESTAMP       NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP,

    -- Ao menos um canal de notificação deve ser informado
    CONSTRAINT chk_canal CHECK (email IS NOT NULL OR whatsapp IS NOT NULL)
);

CREATE INDEX ix_assinantes_ativo     ON assinantes (ativo);
CREATE INDEX ix_assinantes_estado    ON assinantes (estado);

-- ── Histórico de alertas ───────────────────────────────────────────────────
-- Registra cada alerta enviado para evitar re-envios do mesmo lote.
CREATE TABLE historico_alertas (
    id              BIGSERIAL PRIMARY KEY,
    assinante_id    BIGINT      NOT NULL REFERENCES assinantes(id) ON DELETE CASCADE,
    lote_id         BIGINT      NOT NULL REFERENCES lotes(id)      ON DELETE CASCADE,
    canal           VARCHAR(20) NOT NULL,   -- 'EMAIL' ou 'WHATSAPP'
    enviado_em      TIMESTAMP   NOT NULL DEFAULT NOW(),
    sucesso         BOOLEAN     NOT NULL DEFAULT TRUE,
    erro_mensagem   TEXT,

    -- Garante que o mesmo lote não seja enviado duas vezes para o mesmo assinante/canal
    CONSTRAINT uq_historico_assinante_lote_canal
        UNIQUE (assinante_id, lote_id, canal)
);

CREATE INDEX ix_historico_assinante ON historico_alertas (assinante_id);
CREATE INDEX ix_historico_lote      ON historico_alertas (lote_id);
CREATE INDEX ix_historico_enviado   ON historico_alertas (enviado_em);
