ALTER TABLE arbeidsgiver
    ALTER COLUMN identifikator SET NOT NULL,
    ALTER COLUMN organisasjonsnummer DROP NOT NULL;

ALTER TABLE arbeidsforhold
    ALTER COLUMN arbeidsgiver_identifikator SET NOT NULL,
    ALTER COLUMN arbeidsgiver_ref DROP NOT NULL;
ALTER TABLE overstyring_arbeidsforhold
    ALTER COLUMN arbeidsgiver_identifikator SET NOT NULL,
    ALTER COLUMN arbeidsgiver_ref DROP NOT NULL;
ALTER TABLE overstyring_inntekt
    ALTER COLUMN arbeidsgiver_identifikator SET NOT NULL,
    ALTER COLUMN arbeidsgiver_ref DROP NOT NULL;
ALTER TABLE overstyring_minimum_sykdomsgrad_arbeidsgiver
    ALTER COLUMN arbeidsgiver_identifikator SET NOT NULL,
    ALTER COLUMN arbeidsgiver_ref DROP NOT NULL;
ALTER TABLE overstyring_tidslinje
    ALTER COLUMN arbeidsgiver_identifikator SET NOT NULL,
    ALTER COLUMN arbeidsgiver_ref DROP NOT NULL;
ALTER TABLE skjonnsfastsetting_sykepengegrunnlag_arbeidsgiver
    ALTER COLUMN arbeidsgiver_identifikator SET NOT NULL,
    ALTER COLUMN arbeidsgiver_ref DROP NOT NULL;
ALTER TABLE utbetaling_id
    ALTER COLUMN arbeidsgiver_identifikator SET NOT NULL,
    ALTER COLUMN arbeidsgiver_ref DROP NOT NULL;
ALTER TABLE vedtak
    ALTER COLUMN arbeidsgiver_identifikator SET NOT NULL,
    ALTER COLUMN arbeidsgiver_ref DROP NOT NULL;
