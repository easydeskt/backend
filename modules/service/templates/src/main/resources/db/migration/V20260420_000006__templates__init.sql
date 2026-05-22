CREATE TABLE reply_templates (
    id         bigserial    PRIMARY KEY,
    name       varchar(128) NOT NULL UNIQUE,
    content    text,
    created_by uuid         NOT NULL REFERENCES agents(id),
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE reply_template_attachments (
    id           bigserial       PRIMARY KEY,
    template_id  bigint          NOT NULL REFERENCES reply_templates(id) ON DELETE CASCADE,
    kind         attachment_kind NOT NULL,
    file_name    varchar(512)    NOT NULL,
    content_type varchar(128)    NOT NULL,
    file_size    bigint,
    storage_path varchar(1024)   NOT NULL,
    attributes   jsonb           NOT NULL DEFAULT '{}'::jsonb,
    created_at   timestamptz     NOT NULL DEFAULT now()
);

CREATE INDEX idx_reply_template_attachments_template ON reply_template_attachments(template_id);
