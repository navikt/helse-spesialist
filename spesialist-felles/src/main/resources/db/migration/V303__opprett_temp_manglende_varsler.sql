CREATE TABLE temp_manglende_varsler
(
    løpenummer        SERIAL PRIMARY KEY,
    varsel_id         uuid    NOT NULL,
    generasjon_id     uuid    NOT NULL,
    vedtaksperiode_id uuid    NOT NULL,
    låst              BOOLEAN NOT NULL,
    tidspunkt         TIMESTAMP,
    tidspunkt_text    TEXT    NOT NULL,
    tittel            VARCHAR,
    varselkode        VARCHAR
);

CREATE INDEX ON temp_manglende_varsler (varsel_id);
CREATE INDEX ON temp_manglende_varsler (generasjon_id);
CREATE INDEX ON temp_manglende_varsler (vedtaksperiode_id);
CREATE INDEX ON temp_manglende_varsler (varselkode);
