CREATE TYPE actor_kind AS ENUM (
    'AGENT',
    'IDENTITY',
    'SYSTEM'
);

CREATE TYPE ticket_priority AS ENUM (
    'HIGH',
    'LOW',
    'MEDIUM'
);

CREATE TYPE ticket_status AS ENUM (
    'CLOSED',
    'IN_PROGRESS',
    'MERGED',
    'OPEN',
    'RESOLVED'
);

CREATE TABLE tickets (
    id                    bigserial       PRIMARY KEY,
    conversation_id       bigint          NOT NULL REFERENCES conversations(id),
    status                ticket_status   NOT NULL DEFAULT 'OPEN',
    priority              ticket_priority NOT NULL DEFAULT 'MEDIUM',
    assigned_agent_id     uuid            REFERENCES agents(id),
    merged_into_ticket_id bigint          REFERENCES tickets(id),
    attributes            jsonb           NOT NULL DEFAULT '{}'::jsonb,
    created_at            timestamptz     NOT NULL DEFAULT now(),
    updated_at            timestamptz     NOT NULL DEFAULT now(),
    CHECK (merged_into_ticket_id IS NULL OR status = 'MERGED')
);

CREATE INDEX idx_tickets_assigned     ON tickets(assigned_agent_id) WHERE assigned_agent_id IS NOT NULL;
CREATE INDEX idx_tickets_conversation ON tickets(conversation_id);
CREATE INDEX idx_tickets_status       ON tickets(status) WHERE status IN ('OPEN', 'IN_PROGRESS');

CREATE TABLE ticket_supervisor_bindings (
    ticket_id        bigint      NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    supervisor_brand varchar(16) NOT NULL,
    native_id        varchar(256) NOT NULL,
    created_at       timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (ticket_id, supervisor_brand),
    UNIQUE (supervisor_brand, native_id)
);

CREATE TABLE ticket_tags (
    id         bigserial    PRIMARY KEY,
    name       varchar(32)  NOT NULL UNIQUE,
    color      varchar(7),
    created_at timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE ticket_tag_assignments (
    ticket_id         bigint      NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    tag_id            bigint      NOT NULL REFERENCES ticket_tags(id) ON DELETE CASCADE,
    added_by_agent_id uuid        NOT NULL REFERENCES agents(id),
    added_at          timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (ticket_id, tag_id)
);

CREATE TABLE ticket_messages (
    id                    bigserial    PRIMARY KEY,
    ticket_id             bigint       NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    native_id             varchar(256) NOT NULL,
    sender_kind           actor_kind   NOT NULL,
    sender_agent_id       uuid         REFERENCES agents(id),
    sender_identity_id    bigint       REFERENCES channel_identities(id),
    plain_text            text,
    in_reply_to_native_id varchar(256),
    platform_timestamp    timestamptz  NOT NULL,
    attributes            jsonb        NOT NULL DEFAULT '{}'::jsonb,
    created_at            timestamptz  NOT NULL DEFAULT now(),
    UNIQUE (ticket_id, native_id),
    CHECK (
        (sender_kind = 'AGENT'    AND sender_agent_id IS NOT NULL AND sender_identity_id IS NULL)     OR
        (sender_kind = 'IDENTITY' AND sender_agent_id IS NULL     AND sender_identity_id IS NOT NULL) OR
        (sender_kind = 'SYSTEM'   AND sender_agent_id IS NULL     AND sender_identity_id IS NULL)
    )
);

CREATE INDEX idx_ticket_messages_reply  ON ticket_messages(in_reply_to_native_id) WHERE in_reply_to_native_id IS NOT NULL;
CREATE INDEX idx_ticket_messages_ticket ON ticket_messages(ticket_id, platform_timestamp);
