ALTER TABLE arbeidsgiver ALTER COLUMN orgnummer TYPE VARCHAR USING (
CASE
    WHEN LENGTH(orgnummer::varchar) = 10 THEN LPAD(orgnummer::varchar, 11, '0')
    ELSE orgnummer::varchar
    END
);

ALTER TABLE arbeidsgiver RENAME COLUMN orgnummer TO organisasjonsnummer;
CREATE INDEX ON arbeidsgiver(organisasjonsnummer);
