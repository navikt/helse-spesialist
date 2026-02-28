UPDATE arbeidsgiver
SET navn                     = arbeidsgiver_navn.navn,
    navn_sist_oppdatert_dato = arbeidsgiver_navn.navn_oppdatert
FROM arbeidsgiver_navn
WHERE arbeidsgiver.navn_ref = arbeidsgiver_navn.id
  AND arbeidsgiver.navn_ref IS NOT NULL;
