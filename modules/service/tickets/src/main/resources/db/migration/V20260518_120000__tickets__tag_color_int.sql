ALTER TABLE ticket_tags ADD COLUMN color_new integer;

UPDATE ticket_tags
SET color_new = ('x' || substring(color, 2) || 'FF')::bit(32)::integer
WHERE color IS NOT NULL;

ALTER TABLE ticket_tags DROP COLUMN color;
ALTER TABLE ticket_tags RENAME COLUMN color_new TO color;
