DROP TABLE digital_kontaktinformasjon;

CREATE TABLE digital_kontaktinformasjon
(
    person_ref BIGINT    NOT NULL PRIMARY KEY REFERENCES person (id),
    er_digital BOOLEAN   NOT NULL,
    opprettet  TIMESTAMP NOT NULL
);
