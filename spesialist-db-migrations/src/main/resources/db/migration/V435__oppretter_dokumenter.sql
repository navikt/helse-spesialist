CREATE TABLE dokumenter (
    dokument_id                   UUID PRIMARY KEY,
    person_ref                    integer NOT NULL REFERENCES person(id),
    dokument                      JSON NOT NULL,
    opprettet                     TIMESTAMP DEFAULT now() NOT NULL
);