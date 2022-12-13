ALTER TABLE selve_varsel DROP CONSTRAINT selve_varsel_unik_id_key;
CREATE INDEX ON selve_varsel(unik_id);