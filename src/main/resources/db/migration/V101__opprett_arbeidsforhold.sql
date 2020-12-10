CREATE TABLE arbeidsforhold
(
    id               SERIAL PRIMARY KEY,
    person_ref       BIGINT    NOT NULL REFERENCES person (id),
    arbeidsgiver_ref BIGINT    NOT NULL REFERENCES arbeidsgiver (id),
    startdato        DATE      NOT NULL,
    sluttdato        DATE,
    stillingstittel  TEXT      NOT NULL,
    stillingsprosent INT       NOT NULL,
    oppdatert        TIMESTAMP NOT NULL DEFAULT now()
)
