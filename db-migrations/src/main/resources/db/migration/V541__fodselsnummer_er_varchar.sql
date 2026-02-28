ALTER TABLE unnta_fra_automatisk_godkjenning DROP CONSTRAINT unnta_fra_automatisk_godkjenning_fødselsnummer_fkey;

ALTER TABLE unnta_fra_automatisk_godkjenning
ALTER COLUMN fødselsnummer TYPE VARCHAR USING LPAD(fødselsnummer::varchar, 11, '0');

ALTER TABLE person ALTER COLUMN fodselsnummer TYPE VARCHAR USING LPAD(fodselsnummer::varchar, 11, '0');
ALTER TABLE person RENAME COLUMN fodselsnummer TO fødselsnummer;

ALTER TABLE person ALTER COLUMN aktor_id TYPE VARCHAR;
ALTER TABLE person RENAME COLUMN aktor_id TO aktør_id;

ALTER TABLE unnta_fra_automatisk_godkjenning
ADD CONSTRAINT unnta_fra_automatisk_godkjenning_fødselsnummer_fkey
FOREIGN KEY (fødselsnummer) REFERENCES person(fødselsnummer);

CREATE INDEX ON person(fødselsnummer);
CREATE INDEX ON person(aktør_id);
