UPDATE utbetaling_id
SET arbeidsgiver_identifikator = arbeidsgiver.organisasjonsnummer
FROM arbeidsgiver
WHERE utbetaling_id.arbeidsgiver_ref = arbeidsgiver.id;
