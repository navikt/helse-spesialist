ALTER TABLE person ADD COLUMN personinfo_oppdatert DATE DEFAULT now();
ALTER TABLE person_navn DROP COLUMN oppdatert;
