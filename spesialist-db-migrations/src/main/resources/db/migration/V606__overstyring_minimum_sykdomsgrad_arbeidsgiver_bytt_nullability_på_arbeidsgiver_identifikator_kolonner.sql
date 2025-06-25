ALTER TABLE overstyring_minimum_sykdomsgrad_arbeidsgiver
    ALTER COLUMN arbeidsgiver_identifikator SET NOT NULL,
    ALTER COLUMN arbeidsgiver_ref DROP NOT NULL;
