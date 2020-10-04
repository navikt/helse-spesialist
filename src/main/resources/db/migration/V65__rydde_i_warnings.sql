ALTER TABLE warning ADD COLUMN vedtak_ref BIGINT REFERENCES vedtak(id) ON DELETE CASCADE;
UPDATE warning w SET vedtak_ref = vh.vedtaksperiode_ref FROM vedtaksperiode_hendelse vh WHERE w.hendelse_id = vh.hendelse_ref;
ALTER TABLE warning DROP COLUMN hendelse_id;
DELETE FROM warning WHERE vedtak_ref IS NULL;
ALTER TABLE warning ALTER COLUMN vedtak_ref SET NOT NULL;
