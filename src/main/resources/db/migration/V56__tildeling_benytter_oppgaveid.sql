ALTER table tildeling ADD COLUMN oppgave_id_ref bigint;

UPDATE tildeling t SET oppgave_id_ref = o.id FROM oppgave o WHERE o.event_id = t.oppgave_ref;

ALTER table tildeling DROP COLUMN oppgave_ref, ALTER COLUMN oppgave_id_ref SET NOT NULL;

ALTER table tildeling ADD CONSTRAINT tildeling_oppgave_ref_fkey FOREIGN KEY (oppgave_id_ref) references oppgave (id);

