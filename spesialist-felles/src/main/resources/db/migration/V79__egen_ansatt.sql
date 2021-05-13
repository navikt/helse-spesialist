CREATE TABLE egen_ansatt
(
    person_ref     BIGINT PRIMARY KEY REFERENCES person (id),
    er_egen_ansatt BOOLEAN   NOT NULL,
    opprettet      TIMESTAMP NOT NULL
)
