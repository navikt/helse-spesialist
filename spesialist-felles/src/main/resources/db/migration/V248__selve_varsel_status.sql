ALTER TABLE selve_varsel
    ADD COLUMN status_endret_ident VARCHAR DEFAULT NULL;
ALTER TABLE selve_varsel
    ADD COLUMN status VARCHAR NOT NULL DEFAULT 'AKTIV';
ALTER TABLE selve_varsel
    ADD COLUMN status_endret_tidspunkt TIMESTAMPTZ DEFAULT NULL;