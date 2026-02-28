ALTER TABLE arbeidsgiver
    ADD COLUMN identifikator VARCHAR;

ALTER TABLE arbeidsforhold
    ADD COLUMN arbeidsgiver_identifikator VARCHAR;
ALTER TABLE overstyring_arbeidsforhold
    ADD COLUMN arbeidsgiver_identifikator VARCHAR;
ALTER TABLE overstyring_inntekt
    ADD COLUMN arbeidsgiver_identifikator VARCHAR;
ALTER TABLE overstyring_minimum_sykdomsgrad_arbeidsgiver
    ADD COLUMN arbeidsgiver_identifikator VARCHAR;
ALTER TABLE overstyring_tidslinje
    ADD COLUMN arbeidsgiver_identifikator VARCHAR;
ALTER TABLE utbetaling_id
    ADD COLUMN arbeidsgiver_identifikator VARCHAR;
ALTER TABLE vedtak
    ADD COLUMN arbeidsgiver_identifikator VARCHAR;
