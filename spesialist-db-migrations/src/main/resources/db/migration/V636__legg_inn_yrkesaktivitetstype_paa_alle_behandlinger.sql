UPDATE behandling
SET yrkesaktivitetstype ='ARBEIDSTAKER'
FROM vedtak
WHERE vedtak.arbeidsgiver_identifikator != 'SELVSTENDIG'
  AND behandling.vedtaksperiode_id = vedtak.vedtaksperiode_id
  AND behandling.yrkesaktivitetstype IS NULL;

UPDATE behandling
SET yrkesaktivitetstype ='SELVSTENDIG'
FROM vedtak
WHERE vedtak.arbeidsgiver_identifikator = 'SELVSTENDIG'
  AND behandling.vedtaksperiode_id = vedtak.vedtaksperiode_id
  AND behandling.yrkesaktivitetstype IS NULL;
