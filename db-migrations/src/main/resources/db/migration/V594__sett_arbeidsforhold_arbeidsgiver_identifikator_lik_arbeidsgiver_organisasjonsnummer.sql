UPDATE arbeidsforhold
SET arbeidsgiver_identifikator = arbeidsgiver.organisasjonsnummer
FROM arbeidsgiver
WHERE arbeidsforhold.arbeidsgiver_ref = arbeidsgiver.id;
