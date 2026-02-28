ALTER TABLE utbetaling_id
    ALTER COLUMN arbeidsgiver_identifikator SET NOT NULL,
    ALTER COLUMN arbeidsgiver_ref DROP NOT NULL;
