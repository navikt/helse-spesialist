ALTER TABLE skjonnsfastsetting_sykepengegrunnlag_arbeidsgiver
    ALTER COLUMN arbeidsgiver_identifikator SET NOT NULL,
    ALTER COLUMN arbeidsgiver_ref DROP NOT NULL;
