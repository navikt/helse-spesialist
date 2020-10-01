CREATE TABLE digital_kontaktinformasjon
(
    id            SERIAL PRIMARY KEY,
    fodselsnummer BIGINT    NOT NULL,
    er_digital    BOOLEAN   NOT NULL,
    opprettet     TIMESTAMP NOT NULL
);
