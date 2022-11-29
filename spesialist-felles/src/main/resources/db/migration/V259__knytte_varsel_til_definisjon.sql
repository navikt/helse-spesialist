DROP TABLE selve_varsel;
TRUNCATE TABLE selve_vedtaksperiode_generasjon;
CREATE TABLE selve_varsel
(
    id                      BIGSERIAL PRIMARY KEY,
    unik_id                 UUID      NOT NULL UNIQUE,
    kode                    VARCHAR   NOT NULL,
    vedtaksperiode_id       UUID      NOT NULL,
    generasjon_ref          BIGINT    NOT NULL REFERENCES selve_vedtaksperiode_generasjon (id),
    definisjon_ref          BIGINT REFERENCES api_varseldefinisjon (id),
    opprettet               TIMESTAMP NOT NULL,
    status                  VARCHAR   NOT NULL DEFAULT 'AKTIV',
    status_endret_ident     VARCHAR            DEFAULT NULL,
    status_endret_tidspunkt TIMESTAMP          DEFAULT NULL
);