ALTER TABLE selve_vedtaksperiode_generasjon ADD COLUMN spleis_behandling_id uuid;
ALTER TABLE selve_vedtaksperiode_generasjon ADD COLUMN tags VARCHAR[] NOT NULL DEFAULT ARRAY[]::VARCHAR[];