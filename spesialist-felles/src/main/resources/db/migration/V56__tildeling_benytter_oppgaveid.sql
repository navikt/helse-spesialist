TRUNCATE TABLE tildeling;

ALTER TABLE tildeling ADD COLUMN oppgave_id_ref BIGINT;

ALTER TABLE tildeling DROP COLUMN oppgave_ref, ALTER COLUMN oppgave_id_ref SET NOT NULL;

ALTER TABLE tildeling ADD CONSTRAINT tildeling_oppgave_ref_fkey FOREIGN KEY (oppgave_id_ref) REFERENCES oppgave (id);
