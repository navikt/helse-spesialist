CREATE TABLE vergemal
(
    person_ref             BIGINT PRIMARY KEY REFERENCES person (id),
    har_vergemal           BOOLEAN   NOT NULL,
    har_fremtidsfullmakter BOOLEAN   NOT NULL,
    har_fullmakter         BOOLEAN   NOT NULL,
    opprettet              TIMESTAMP NOT NULL
)
