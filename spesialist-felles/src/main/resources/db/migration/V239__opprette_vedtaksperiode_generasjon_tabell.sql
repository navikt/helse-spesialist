CREATE TABLE selve_vedtaksperiode_generasjon
(
    id                BIGSERIAL PRIMARY KEY,
    vedtaksperiode_id UUID      NOT NULL,
    låst              BOOLEAN NOT NULL DEFAULT FALSE,
    låst_tidspunkt    TIMESTAMPTZ DEFAULT NULL,
    opprettet         TIMESTAMPTZ DEFAULT now()
);
