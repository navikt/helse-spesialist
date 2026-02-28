DELETE
FROM arbeidsgiver
WHERE navn IS NULL
   OR navn_sist_oppdatert_dato IS NULL;
