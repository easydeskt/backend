ALTER TABLE agents
    ALTER COLUMN role DROP DEFAULT,
    ALTER COLUMN role TYPE varchar(16) USING role::varchar,
    ALTER COLUMN role SET DEFAULT 'OPERATOR';

DROP TYPE agent_role;
