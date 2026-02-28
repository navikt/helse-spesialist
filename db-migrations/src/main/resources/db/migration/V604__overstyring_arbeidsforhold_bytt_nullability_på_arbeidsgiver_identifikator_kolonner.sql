ALTER TABLE overstyring_arbeidsforhold
    ALTER COLUMN arbeidsgiver_identifikator SET NOT NULL,
    ALTER COLUMN arbeidsgiver_ref DROP NOT NULL;
