ALTER TABLE hendelse DROP COLUMN original, DROP COLUMN spleis_referanse;
ALTER TABLE hendelse RENAME CONSTRAINT spleisbehov_pkey TO hendelse_pkey;
