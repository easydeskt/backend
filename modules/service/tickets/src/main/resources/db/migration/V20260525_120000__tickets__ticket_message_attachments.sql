-- attachment_kind ENUM is owned by service:storage migrations — reuse it here.
CREATE TABLE ticket_message_attachments (
    id             BIGSERIAL       PRIMARY KEY,
    message_id     BIGINT          NOT NULL,
    kind           attachment_kind NOT NULL,
    file_name      VARCHAR(512)    NOT NULL,
    content_type   VARCHAR(128)    NOT NULL,
    file_size      BIGINT,
    channel_brand  VARCHAR(64)     NOT NULL,
    attributes     JSONB           NOT NULL DEFAULT '{}',
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_ticket_message_attachments_message_id ON ticket_message_attachments (message_id);
