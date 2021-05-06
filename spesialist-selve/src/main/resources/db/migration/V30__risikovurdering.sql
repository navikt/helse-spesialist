CREATE TABLE risikovurdering
(
    id SERIAL PRIMARY KEY,
    vedtaksperiode_id UUID NOT NULL,
    samlet_score INTEGER NOT NULL,
    ufullstendig BOOLEAN NOT NULL
);

CREATE TABLE risikovurdering_begrunnelse
(
    id                  SERIAL PRIMARY KEY,
    risikovurdering_ref INTEGER REFERENCES risikovurdering (id),
    begrunnelse         TEXT NOT NULL
)

