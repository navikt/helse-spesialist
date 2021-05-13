CREATE TABLE automatisering_problem
(
    id                 SERIAL NOT NULL PRIMARY KEY,
    vedtaksperiode_ref INT    NOT NULL REFERENCES vedtak (id),
    hendelse_ref       UUID   NOT NULL REFERENCES hendelse (id),
    problem            VARCHAR(100) NOT NULL
);
