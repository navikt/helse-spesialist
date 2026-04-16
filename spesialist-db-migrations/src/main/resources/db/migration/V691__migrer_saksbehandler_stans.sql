-- Migrerer data fra den gamle stans_automatisk_behandling_saksbehandler-tabellen
-- til den nye saksbehandler_stans-tabellen.
-- Bruker periodehistorikk til å hente saksbehandler-ident og begrunnelse.

-- Hent siste stans-historikkinnslag per person
WITH siste_stans_historikk AS (
    SELECT DISTINCT ON (p.fødselsnummer)
        p.fødselsnummer,
        s.ident            AS utfort_av_ident,
        ph.json ->> 'notattekst' AS begrunnelse,
        ph.timestamp       AS historikk_tidspunkt
    FROM periodehistorikk ph
             JOIN behandling b ON b.unik_id = ph.behandling_id
             JOIN vedtaksperiode vp ON vp.vedtaksperiode_id = b.vedtaksperiode_id
             JOIN person p ON p.id = vp.person_ref
             LEFT JOIN saksbehandler s ON s.oid = ph.saksbehandler_oid
    WHERE ph.type = 'STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER'
    ORDER BY p.fødselsnummer, ph.timestamp DESC
),
-- Hent siste opphev-historikkinnslag per person
siste_opphev_historikk AS (
    SELECT DISTINCT ON (p.fødselsnummer)
        p.fødselsnummer,
        s.ident                  AS opphevet_av_ident,
        ph.json ->> 'notattekst' AS opphevet_begrunnelse,
        ph.timestamp             AS opphevet_tidspunkt
    FROM periodehistorikk ph
             JOIN behandling b ON b.unik_id = ph.behandling_id
             JOIN vedtaksperiode vp ON vp.vedtaksperiode_id = b.vedtaksperiode_id
             JOIN person p ON p.id = vp.person_ref
             LEFT JOIN saksbehandler s ON s.oid = ph.saksbehandler_oid
    WHERE ph.type = 'OPPHEV_STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER'
    ORDER BY p.fødselsnummer, ph.timestamp DESC
)
INSERT INTO saksbehandler_stans (id, identitetsnummer, utfort_av_ident, begrunnelse, opprettet,
                                 opphevet_av_ident, opphevet_begrunnelse, opphevet_tidspunkt)
SELECT gen_random_uuid(),
       old.fødselsnummer,
       COALESCE(sh.utfort_av_ident, 'UKJENT'),
       COALESCE(sh.begrunnelse, 'Migrert fra stans_automatisk_behandling_saksbehandler'),
       old.opprettet,
       oh.opphevet_av_ident,
       oh.opphevet_begrunnelse,
       oh.opphevet_tidspunkt
FROM stans_automatisk_behandling_saksbehandler old
         LEFT JOIN siste_stans_historikk sh ON sh.fødselsnummer = old.fødselsnummer
         LEFT JOIN siste_opphev_historikk oh ON oh.fødselsnummer = old.fødselsnummer
             -- Bare ta med opphev-innslag som skjedde etter selve stansen
             AND oh.opphevet_tidspunkt > COALESCE(sh.historikk_tidspunkt, old.opprettet)
WHERE old.fødselsnummer NOT IN (SELECT identitetsnummer FROM saksbehandler_stans);
