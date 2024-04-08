DROP INDEX IF EXISTS selve_vedtaksperiode_generasjon_tilstand_idx;

CREATE INDEX ON selve_vedtaksperiode_generasjon(tilstand) WHERE utbetaling_id IS NOT NULL;