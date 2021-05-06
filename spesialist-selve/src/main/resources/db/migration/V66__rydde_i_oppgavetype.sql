ALTER TABLE saksbehandleroppgavetype ADD COLUMN vedtak_ref BIGINT REFERENCES vedtak(id) ON DELETE CASCADE;
UPDATE saksbehandleroppgavetype s SET vedtak_ref = vh.vedtaksperiode_ref FROM vedtaksperiode_hendelse vh WHERE s.hendelse_id = vh.hendelse_ref;
ALTER TABLE saksbehandleroppgavetype DROP COLUMN hendelse_id;
DELETE FROM saksbehandleroppgavetype WHERE vedtak_ref IS NULL;
ALTER TABLE saksbehandleroppgavetype ALTER COLUMN vedtak_ref SET NOT NULL;
