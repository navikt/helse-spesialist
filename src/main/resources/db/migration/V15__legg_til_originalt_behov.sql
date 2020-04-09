ALTER TABLE spleisbehov ADD COLUMN original json NOT NULL DEFAULT '{}';
ALTER TABLE spleisbehov ALTER original DROP DEFAULT;
