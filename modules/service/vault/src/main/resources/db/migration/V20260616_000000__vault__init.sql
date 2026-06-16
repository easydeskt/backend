CREATE TABLE vault_secrets (
    id              bigserial    PRIMARY KEY,
    name            varchar(64)  NOT NULL,
    description     text         NULL,
    encrypted_value text         NOT NULL,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_vault_secrets_name UNIQUE (name)
);
