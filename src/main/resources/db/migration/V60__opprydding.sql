ALTER TABLE warning DROP CONSTRAINT warning_spleisbehov_ref_fkey;
ALTER TABLE warning ADD CONSTRAINT warning_hendelse_ref_fkey FOREIGN KEY(hendelse_id) REFERENCES hendelse(id) ON DELETE CASCADE;

DELETE FROM hendelse WHERE fodselsnummer IS NULL OR type = 'Godkjenningsbehov';
ALTER TABLE hendelse DROP COLUMN original, DROP COLUMN spleis_referanse, ALTER COLUMN fodselsnummer SET NOT NULL;
ALTER TABLE hendelse RENAME CONSTRAINT spleisbehov_pkey TO hendelse_pkey;

DROP TABLE command,person_metadata, person_metadata_json, person_egenskap, person_egenskap_type;
