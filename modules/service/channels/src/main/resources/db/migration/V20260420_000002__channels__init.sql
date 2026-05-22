CREATE TABLE channels (
    id           bigserial    PRIMARY KEY,
    brand        varchar(16)  NOT NULL,
    display_name varchar(128)  NOT NULL,
    config       jsonb         NOT NULL,
    attributes   jsonb         NOT NULL DEFAULT '{}'::jsonb,
    is_enabled   boolean       NOT NULL DEFAULT true,
    created_at   timestamptz   NOT NULL DEFAULT now(),
    updated_at   timestamptz   NOT NULL DEFAULT now()
);

CREATE INDEX idx_channels_brand_enabled ON channels(brand, is_enabled);

CREATE TABLE channel_identities (
    id            bigserial     PRIMARY KEY,
    channel_brand varchar(16)   NOT NULL,
    native_id     varchar(256)  NOT NULL,
    display_name  varchar(256),
    first_seen_at timestamptz   NOT NULL DEFAULT now(),
    last_seen_at  timestamptz   NOT NULL DEFAULT now(),
    UNIQUE (channel_brand, native_id)
);

CREATE TABLE identity_notes (
    id              bigserial   PRIMARY KEY,
    identity_id     bigint      NOT NULL REFERENCES channel_identities(id) ON DELETE CASCADE,
    text            text        NOT NULL,
    author_agent_id uuid        NOT NULL REFERENCES agents(id),
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_identity_notes_identity ON identity_notes(identity_id, created_at DESC);

CREATE TABLE conversations (
    id          bigserial   PRIMARY KEY,
    channel_id  bigint      NOT NULL REFERENCES channels(id),
    identity_id bigint      NOT NULL REFERENCES channel_identities(id),
    attributes  jsonb       NOT NULL DEFAULT '{}'::jsonb,
    created_at  timestamptz NOT NULL DEFAULT now(),
    UNIQUE (channel_id, identity_id)
);
