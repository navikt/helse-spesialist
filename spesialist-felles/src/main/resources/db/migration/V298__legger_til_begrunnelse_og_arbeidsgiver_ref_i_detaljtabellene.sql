-- Legger til nye felter i overstyring_inntekt og overstyring_arbeidsforhold
ALTER TABLE overstyring_inntekt
    ADD COLUMN arbeidsgiver_ref INT REFERENCES arbeidsgiver (id),
    ADD COLUMN begrunnelse TEXT,
    ADD COLUMN subsumsjon JSON;

ALTER TABLE overstyring_arbeidsforhold
    ADD COLUMN arbeidsgiver_ref INT REFERENCES arbeidsgiver (id),
    ADD COLUMN begrunnelse TEXT;
