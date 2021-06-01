CREATE TABLE feilende_meldinger (
    id uuid NOT NULL PRIMARY KEY,
    event_name varchar(40) NOT NULL,
    opprettet timestamp NOT NULL DEFAULT now(),
    blob json NOT NULL
)
