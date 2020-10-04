DELETE FROM hendelse WHERE fodselsnummer IS NULL;
ALTER TABLE hendelse ALTER COLUMN fodselsnummer SET NOT NULL;
