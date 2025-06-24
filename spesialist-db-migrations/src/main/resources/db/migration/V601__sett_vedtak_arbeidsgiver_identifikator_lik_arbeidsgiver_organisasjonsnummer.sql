UPDATE vedtak
SET arbeidsgiver_identifikator = arbeidsgiver.organisasjonsnummer
FROM arbeidsgiver
WHERE arbeidsgiver_identifikator IS NULL
  AND vedtak.arbeidsgiver_ref = arbeidsgiver.id;
