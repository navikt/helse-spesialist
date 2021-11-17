ALTER TABLE person_info ADD COLUMN adressebeskyttelse VARCHAR(32) DEFAULT 'Ugradert';
ALTER TABLE person_info ALTER COLUMN adressebeskyttelse DROP DEFAULT;
