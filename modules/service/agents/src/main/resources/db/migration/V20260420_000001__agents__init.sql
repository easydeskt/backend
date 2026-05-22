CREATE TYPE agent_role AS ENUM (
    'ADMIN',
    'OPERATOR'
);

CREATE TABLE agents (
    id                uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name      varchar(128) NOT NULL,
    role              agent_role   NOT NULL DEFAULT 'OPERATOR',
    is_active         boolean      NOT NULL DEFAULT true,
    added_by_agent_id uuid         REFERENCES agents(id),
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_at        timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_agents_active ON agents(is_active) WHERE is_active;

CREATE TABLE agent_supervisor_bindings (
    agent_id         uuid         NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
    supervisor_brand varchar(16)  NOT NULL,
    native_id        varchar(256) NOT NULL,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (agent_id, supervisor_brand),
    UNIQUE (supervisor_brand, native_id)
);
