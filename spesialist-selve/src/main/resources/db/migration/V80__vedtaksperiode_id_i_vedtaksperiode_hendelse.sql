ALTER TABLE vedtaksperiode_hendelse
    ADD COLUMN vedtaksperiode_id uuid;

UPDATE vedtaksperiode_hendelse SET vedtaksperiode_id = v.vedtaksperiode_id FROM vedtak v WHERE v.id = vedtaksperiode_ref;

ALTER TABLE vedtaksperiode_hendelse DROP CONSTRAINT vedtaksperiode_hendelse_vedtaksperiode_ref_fkey;
ALTER TABLE vedtaksperiode_hendelse DROP COLUMN vedtaksperiode_ref;

ALTER TABLE vedtaksperiode_hendelse ALTER COLUMN vedtaksperiode_id SET NOT NULL;
