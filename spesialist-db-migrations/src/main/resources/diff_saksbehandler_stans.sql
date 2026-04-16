
-- Diff-spørring: Sjekker om alle rader i den gamle tabellen finnes i den nye
-- Kjør etter V691 for å verifisere migreringen

-- 1. Rader i gammel tabell som IKKE finnes i ny tabell (bør være 0 etter migrering)
SELECT 'Kun i gammel tabell' AS kilde, old.fødselsnummer, old.opprettet, NULL AS id
FROM stans_automatisk_behandling_saksbehandler old
         LEFT JOIN saksbehandler_stans ny ON old.fødselsnummer = ny.identitetsnummer
WHERE ny.identitetsnummer IS NULL

UNION ALL

-- 2. Rader i ny tabell som IKKE har opphav i gammel tabell (nye stans opprettet direkte i ny tabell)
SELECT 'Kun i ny tabell' AS kilde, ny.identitetsnummer, ny.opprettet, ny.id::text
FROM saksbehandler_stans ny
         LEFT JOIN stans_automatisk_behandling_saksbehandler old ON ny.identitetsnummer = old.fødselsnummer
WHERE old.fødselsnummer IS NULL

ORDER BY kilde, opprettet;

-- 3. Oppsummering: antall i hver tabell + antall som er migrert
SELECT
    (SELECT count(*) FROM stans_automatisk_behandling_saksbehandler) AS antall_gammel_tabell,
    (SELECT count(*) FROM saksbehandler_stans)                      AS antall_ny_tabell,
    (SELECT count(*)
     FROM stans_automatisk_behandling_saksbehandler old
              INNER JOIN saksbehandler_stans ny ON old.fødselsnummer = ny.identitetsnummer
    )                                                                AS antall_migrert;

-- 4. Kvalitetssjekk: Hvor mange migrerte rader fikk data fra periodehistorikk vs fallback?
SELECT
    count(*) FILTER (WHERE utfort_av_ident != 'UKJENT')  AS med_saksbehandler_fra_historikk,
    count(*) FILTER (WHERE utfort_av_ident = 'UKJENT')   AS uten_saksbehandler_fallback,
    count(*) FILTER (WHERE opphevet_tidspunkt IS NOT NULL) AS antall_opphevet
FROM saksbehandler_stans ny
WHERE EXISTS (
    SELECT 1 FROM stans_automatisk_behandling_saksbehandler old
    WHERE old.fødselsnummer = ny.identitetsnummer
);
