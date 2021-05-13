CREATE TABLE overstyring
(
    id                  SERIAL PRIMARY KEY,
    organisasjonsnummer BIGINT NOT NULL,
    fodselsnummer       BIGINT NOT NULL,
    begrunnelse         TEXT    NOT NULL,
    unntaFraInnsyn      BOOLEAN NOT NULL,
    overstyrteDager     JSONB   NOT NULL
)
