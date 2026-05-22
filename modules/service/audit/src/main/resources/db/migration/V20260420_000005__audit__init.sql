CREATE TABLE audit_events (
    id         bigserial   PRIMARY KEY,
    ticket_id  bigint      REFERENCES tickets(id) ON DELETE SET NULL,
    agent_id   uuid        REFERENCES agents(id),
    event_type varchar(64) NOT NULL,
    payload    jsonb       NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_events_created ON audit_events(created_at DESC);
CREATE INDEX idx_audit_events_ticket  ON audit_events(ticket_id, created_at DESC) WHERE ticket_id IS NOT NULL;
CREATE INDEX idx_audit_events_type    ON audit_events(event_type, created_at DESC);
