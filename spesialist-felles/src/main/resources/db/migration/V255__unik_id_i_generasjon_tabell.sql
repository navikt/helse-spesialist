DROP TABLE selve_vedtaksperiode_generasjon;
CREATE TABLE selve_vedtaksperiode_generasjon
(
    id                    BIGSERIAL PRIMARY KEY,
    unik_id               UUID    NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    vedtaksperiode_id     UUID    NOT NULL,
    opprettet_tidspunkt   TIMESTAMPTZ      DEFAULT now(),
    opprettet_av_hendelse UUID    NOT NULL,
    låst_tidspunkt        TIMESTAMPTZ      DEFAULT NULL,
    låst_av_hendelse      UUID             DEFAULT NULL,
    låst                  BOOLEAN NOT NULL DEFAULT FALSE
);
