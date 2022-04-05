ALTER TABLE warning
    ADD COLUMN opprettet timestamp;
ALTER TABLE warning
    ADD COLUMN inaktiv_fra timestamp;

-- Setter default-verdi for eksisterende rader ettersom vi ikke umiddelbart ser noen måte å finne frem korrekt verdi for hvert enkelt tilfelle.
UPDATE warning
SET opprettet='1970-01-01 00:00:00'::timestamp;

ALTER TABLE warning
    ALTER COLUMN opprettet SET NOT NULL;