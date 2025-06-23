UPDATE overstyring_tidslinje
SET arbeidsgiver_identifikator = arbeidsgiver.organisasjonsnummer
FROM arbeidsgiver
WHERE overstyring_tidslinje.arbeidsgiver_ref IS NOT NULL
  AND overstyring_tidslinje.arbeidsgiver_ref = arbeidsgiver.id;
