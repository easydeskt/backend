-- attachment_kind ENUM is owned by service:storage migrations — reuse it here.
CREATE TABLE ticket_message_attachments (
    id             bigserial       PRIMARY KEY,
    message_id     bigint          NOT NULL,
    kind           attachment_kind NOT NULL,
    file_name      varchar(512)    NOT NULL,
    content_type   varchar(128)    NOT NULL,
    file_size      bigint,
    channel_brand  varchar(64)     NOT NULL,
    attributes     jsonb           NOT NULL DEFAULT '{}'::jsonb,
    created_at     timestamptz     NOT NULL DEFAULT now(),
    CONSTRAINT fk_ticket_message_attachments_message
        FOREIGN KEY (message_id) REFERENCES ticket_messages(id) ON DELETE CASCADE
);

CREATE INDEX idx_ticket_message_attachments_message_id ON ticket_message_attachments (message_id);
