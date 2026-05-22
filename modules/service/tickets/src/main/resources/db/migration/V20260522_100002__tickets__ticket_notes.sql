CREATE TABLE ticket_notes (
    id              bigserial   PRIMARY KEY,
    ticket_id       bigint      NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_agent_id uuid        NOT NULL REFERENCES agents(id),
    text            text        NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_ticket_notes_ticket ON ticket_notes(ticket_id);
