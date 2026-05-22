ALTER TABLE agent_supervisor_bindings
    ADD COLUMN attributes jsonb NOT NULL DEFAULT '{}'::jsonb;
