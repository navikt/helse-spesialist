ALTER TABLE vergemal RENAME COLUMN opprettet to vergemål_oppdatert;
ALTER TABLE vergemal ADD COLUMN fullmakt_oppdatert TIMESTAMP;
UPDATE vergemal SET fullmakt_oppdatert=vergemål_oppdatert;
ALTER TABLE vergemal ALTER COLUMN fullmakt_oppdatert SET NOT NULL;
