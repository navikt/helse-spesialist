CREATE TABLE gosysoppgaver
(
    person_ref     BIGINT    NOT NULL PRIMARY KEY REFERENCES person (id),
    antall         INT,
    oppslag_feilet BOOLEAN   NOT NULL,
    opprettet      TIMESTAMP NOT NULL
);
