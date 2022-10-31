DROP TABLE selve_varsel;
CREATE TABLE selve_varsel
(
    id                BIGSERIAL PRIMARY KEY,
    unik_id           UUID      NOT NULL UNIQUE,
    kode              VARCHAR   NOT NULL,
    vedtaksperiode_id UUID      NOT NULL,
    opprettet         TIMESTAMPTZ NOT NULL
);