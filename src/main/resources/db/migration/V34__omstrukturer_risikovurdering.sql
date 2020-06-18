DROP TABLE risikovurdering_begrunnelse;
DROP TABLE risikovurdering;

CREATE TABLE risikovurdering
(
    id                SERIAL PRIMARY KEY,
    vedtaksperiode_id UUID      NOT NULL,
    samlet_score      INT       NOT NULL,
    ufullstendig      BOOLEAN   NOT NULL,
    opprettet         TIMESTAMP NOT NULL
);

CREATE TABLE risikovurdering_faresignal
(
    id               SERIAL PRIMARY KEY,
    risikovurdering_ref INT REFERENCES risikovurdering (id),
    tekst            TEXT NOT NULL
);

CREATE TABLE risikovurdering_arbeidsuforhetvurdering
(
    id               SERIAL PRIMARY KEY,
    risikovurdering_ref INT REFERENCES risikovurdering (id),
    tekst            TEXT NOT NULL
);
