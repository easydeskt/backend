ALTER TABLE tickets ADD COLUMN assigned_at           timestamptz;
ALTER TABLE tickets ADD COLUMN closed_at             timestamptz;
ALTER TABLE tickets ADD COLUMN merged_at             timestamptz;
ALTER TABLE tickets ADD COLUMN read_up_to_message_id bigint;
ALTER TABLE tickets ADD COLUMN resolved_at           timestamptz;
