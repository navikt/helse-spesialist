CREATE TABLE api_varseldefinisjon
(
    id         BIGSERIAL PRIMARY KEY,
    unik_id    UUID UNIQUE           NOT NULL,
    kode       VARCHAR               NOT NULL,
    tittel     VARCHAR               NOT NULL,
    forklaring VARCHAR,
    handling   VARCHAR,
    avviklet   VARCHAR DEFAULT FALSE NOT NULL,
    opprettet  TIMESTAMPTZ           NOT NULL
);

CREATE INDEX ON api_varseldefinisjon (kode);