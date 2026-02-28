UPDATE overstyring_inntekt
SET arbeidsgiver_identifikator = arbeidsgiver.organisasjonsnummer
FROM arbeidsgiver
WHERE overstyring_inntekt.arbeidsgiver_ref = arbeidsgiver.id;
