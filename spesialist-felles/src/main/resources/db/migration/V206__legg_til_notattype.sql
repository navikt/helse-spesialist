-- Alle eksisterende notater ved migrering er av typen PåVent.
-- Fremover vil vi defaulte til Generelt hvis ikke noe annet er spesifisert, derfor trikser vi litt i migreringen.

CREATE TYPE notattype AS ENUM ('Retur', 'Generelt', 'PaaVent');

ALTER table notat ADD COLUMN type notattype;

UPDATE notat SET type = 'PaaVent';

ALTER TABLE notat ALTER COLUMN type SET NOT NULL;
ALTER TABLE notat ALTER COLUMN type SET DEFAULT 'Generelt';
