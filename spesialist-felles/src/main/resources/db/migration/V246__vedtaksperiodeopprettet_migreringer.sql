ALTER TABLE person ALTER COLUMN personinfo_oppdatert SET DEFAULT null;
ALTER TABLE person ALTER COLUMN enhet_ref_oppdatert SET DEFAULT null;
ALTER TABLE person ALTER COLUMN infotrygdutbetalinger_oppdatert DROP NOT NULL;
ALTER TABLE person ALTER COLUMN infotrygdutbetalinger_oppdatert SET DEFAULT null;

ALTER TABLE arbeidsgiver ALTER COLUMN navn_ref DROP NOT NULL;