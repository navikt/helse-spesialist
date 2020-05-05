CREATE TYPE person_kjonn AS ENUM ('Mann', 'Kvinne', 'Ukjent');
ALTER TABLE person_navn
    RENAME TO person_info;

ALTER TABLE person_info
    ADD COLUMN fodselsdato DATE,
    ADD COLUMN kjonn       person_kjonn;

ALTER TABLE person
    RENAME COLUMN navn_ref TO info_ref
