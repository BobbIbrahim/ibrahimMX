ALTER TABLE squad
    ADD COLUMN type VARCHAR(64);

UPDATE squad
SET type = 'hardcoded-flow'
WHERE type IS NULL;

ALTER TABLE squad
    ALTER COLUMN type SET NOT NULL;