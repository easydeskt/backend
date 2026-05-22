CREATE TYPE attachment_kind AS ENUM (
    'AUDIO',
    'DOCUMENT',
    'PHOTO',
    'STICKER',
    'VIDEO',
    'VOICE'
);

CREATE TABLE attachments (
    id           bigserial       PRIMARY KEY,
    message_id   bigint          NOT NULL,
    kind         attachment_kind NOT NULL,
    file_name    varchar(512)    NOT NULL,
    content_type varchar(128)    NOT NULL,
    file_size    bigint,
    storage_path varchar(1024)   NOT NULL,
    attributes   jsonb           NOT NULL DEFAULT '{}'::jsonb,
    created_at   timestamptz     NOT NULL DEFAULT now()
);

CREATE INDEX idx_attachments_message ON attachments(message_id);
