UPDATE utbetaling_id
SET arbeidsgiver_identifikator = arbeidsgiver.organisasjonsnummer
FROM arbeidsgiver
WHERE arbeidsgiver_identifikator IS NULL
  AND utbetaling_id.arbeidsgiver_ref = arbeidsgiver.id;
