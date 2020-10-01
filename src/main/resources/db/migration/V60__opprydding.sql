ALTER TABLE warning ADD COLUMN vedtak_ref BIGINT REFERENCES vedtak(id) ON DELETE CASCADE;
UPDATE warning w SET vedtak_ref = o.vedtak_ref FROM oppgave o WHERE w.hendelse_id = o.hendelse_id;
ALTER TABLE warning DROP COLUMN hendelse_id;
ALTER TABLE warning ALTER COLUMN vedtak_ref SET NOT NULL;

ALTER TABLE saksbehandleroppgavetype ADD COLUMN vedtak_ref BIGINT REFERENCES vedtak(id) ON DELETE CASCADE;
UPDATE saksbehandleroppgavetype s SET vedtak_ref = o.vedtak_ref FROM oppgave o WHERE s.hendelse_id = o.hendelse_id;
ALTER TABLE saksbehandleroppgavetype DROP COLUMN hendelse_id;
ALTER TABLE saksbehandleroppgavetype ALTER COLUMN vedtak_ref SET NOT NULL;

ALTER TABLE command_context DROP CONSTRAINT command_context_spleisbehov_id_fkey;
ALTER TABLE command_context ADD CONSTRAINT command_context_hendelse_id_fkey FOREIGN KEY(hendelse_id) REFERENCES hendelse(id) ON DELETE CASCADE;

ALTER TABLE vedtaksperiode_hendelse DROP CONSTRAINT vedtaksperiode_hendelse_hendelse_ref_fkey;
ALTER TABLE vedtaksperiode_hendelse ADD CONSTRAINT vedtaksperiode_hendelse_hendelse_ref_fkey FOREIGN KEY(hendelse_ref) REFERENCES hendelse(id) ON DELETE CASCADE;

ALTER TABLE oppgave DROP COLUMN hendelse_id;

DELETE FROM hendelse WHERE fodselsnummer IS NULL OR type = 'Godkjenningsbehov';
ALTER TABLE hendelse DROP COLUMN original, DROP COLUMN spleis_referanse, ALTER COLUMN fodselsnummer SET NOT NULL;
ALTER TABLE hendelse RENAME CONSTRAINT spleisbehov_pkey TO hendelse_pkey;

DROP TABLE command,person_metadata, person_metadata_json, person_egenskap, person_egenskap_type;
