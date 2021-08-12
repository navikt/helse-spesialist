CREATE TABLE overstyring_inntekt
(
    id                 SERIAL PRIMARY KEY,
    tidspunkt          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    person_ref         INT            NOT NULL REFERENCES person (id),
    arbeidsgiver_ref   INT            NOT NULL REFERENCES arbeidsgiver (id),
    saksbehandler_ref  UUID           NOT NULL REFERENCES saksbehandler (oid),
    hendelse_ref     UUID           NOT NULL REFERENCES hendelse (id),
    begrunnelse        TEXT           NOT NULL,
    manedlig_inntekt   NUMERIC(12, 2) NOT NULL,
    skjaeringstidspunkt date
)
