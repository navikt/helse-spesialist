CREATE TABLE overstyring_arbeidsforhold
(
    id                  SERIAL PRIMARY KEY,
    tidspunkt           TIMESTAMPTZ NOT NULL DEFAULT now(),
    person_ref          INT         NOT NULL REFERENCES person (id),
    arbeidsgiver_ref    INT         NOT NULL REFERENCES arbeidsgiver (id),
    saksbehandler_ref   UUID        NOT NULL REFERENCES saksbehandler (oid),
    hendelse_ref        UUID        NOT NULL REFERENCES hendelse (id),
    begrunnelse         TEXT        NOT NULL,
    forklaring          TEXT        NOT NULL,
    er_aktivt           BOOLEAN     NOT NULL,
    skjaeringstidspunkt date
)
