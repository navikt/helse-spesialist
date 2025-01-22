-- Holder på alle periodehistorikk-innslag som vi anser som relevante å knytte til generasjon_id
-- Denne brukes som arbeidstabell mens migreringen pågår.
-- Oppdaterer til slutt periodehistorikk-tabellen fra denne tabellen
CREATE temp table innslag_som_enda_ikke_er_knyttet_til_generasjon AS (
    select DISTINCT ON (p.id) p.*, v.vedtaksperiode_id
    from periodehistorikk p
             inner join vedtaksperiode_utbetaling_id vui using (utbetaling_id)
             inner join vedtak v using (vedtaksperiode_id)
    where p.generasjon_id is null
);

-- Knytter alle innslag som kan knyttes til en generasjon vha. utbetaling_id til riktig generasjon
UPDATE innslag_som_enda_ikke_er_knyttet_til_generasjon iseiektg SET generasjon_id = svg.unik_id
FROM selve_vedtaksperiode_generasjon svg WHERE svg.utbetaling_id = iseiektg.utbetaling_id AND svg.vedtaksperiode_id = iseiektg.vedtaksperiode_id
                                           AND iseiektg.generasjon_id IS NULL;

-- Indekser for fart
CREATE INDEX tmp_iseiektg_i ON innslag_som_enda_ikke_er_knyttet_til_generasjon (id);
CREATE INDEX tmp_iseiektg_v ON innslag_som_enda_ikke_er_knyttet_til_generasjon (vedtaksperiode_id);
CREATE INDEX tmp_iseiektg_u ON innslag_som_enda_ikke_er_knyttet_til_generasjon (utbetaling_id);

-- Forsøker å innplassere alle innslag som vi ikke kunne knytte til en generasjon vha. utbetaling_id
-- basert på tid i stedet, dvs. et innslag som ble opprettet mellom to generasjoner hører til den tidligste av de
-- Denne update'n vil kun knytte innslag til alt som ikke er siste generasjon, fordi vi sjekker mellom to opprettet-tidspunkter
WITH vinduer AS (
    SELECT
        isikktv.id,
        unik_id,
        opprettet_tidspunkt as opprettet,
        LAG (unik_id, 1) OVER (
            PARTITION BY svg.vedtaksperiode_id
            ORDER BY opprettet_tidspunkt
            ) as forrige_unik_id,
        LAG (opprettet_tidspunkt, 1) OVER (
            PARTITION BY svg.vedtaksperiode_id
            ORDER BY opprettet_tidspunkt
            ) as forrige_opprettet
    FROM selve_vedtaksperiode_generasjon svg
             INNER JOIN innslag_som_enda_ikke_er_knyttet_til_generasjon isikktv using (vedtaksperiode_id)
    WHERE isikktv.generasjon_id IS NULL
)
UPDATE innslag_som_enda_ikke_er_knyttet_til_generasjon iseiektg SET generasjon_id = v.unik_id
FROM vinduer v WHERE iseiektg.id = v.id AND iseiektg.timestamp BETWEEN v.opprettet AND v.forrige_opprettet AND v.forrige_opprettet IS NOT NULL;

-- Holder på alle gjeldende generasjoner for vedtaksperioder som har periodehistorikk som må migreres
create temporary table nyeste_generasjon_for_periodehistorikk as
select nyeste_generasjon.unik_id, nyeste_generasjon.vedtaksperiode_id
from (select DISTINCT ON (vedtaksperiode_id) unik_id, vedtaksperiode_id
      from selve_vedtaksperiode_generasjon svg
      ORDER BY vedtaksperiode_id, opprettet_tidspunkt DESC) nyeste_generasjon
         INNER JOIN innslag_som_enda_ikke_er_knyttet_til_generasjon USING (vedtaksperiode_id);

CREATE INDEX tmp_ngfp_v ON nyeste_generasjon_for_periodehistorikk(vedtaksperiode_id);

-- Vi antar at alle innslag som fortsatt ikke er knyttet til en generasjon hører til den siste
-- og knytter disse til siste generasjon
UPDATE innslag_som_enda_ikke_er_knyttet_til_generasjon iseiektg SET generasjon_id = ngfp.unik_id
FROM nyeste_generasjon_for_periodehistorikk ngfp
WHERE ngfp.vedtaksperiode_id = iseiektg.vedtaksperiode_id AND iseiektg.generasjon_id IS NULL;

-- Til sist oppdaterer vi periodehistorikk fra arbeidstabellen
-- Det som ligger igjen i periodehistorikk uten knytning til generasjonen er ting som har blitt
-- forkastet og som det ikke finnes noen generasjoner i Spesialist for (alt er fra 2022 og bakover).
-- Dette utgjør omtrent 10.000 innslag.
-- I tillegg er det 25 raringer som hører til nye generasjoner (fra 2024) hvor alle innslagene er av typen AUTOMATISK_BEHANDLING_STANSET.
UPDATE periodehistorikk p SET generasjon_id = iseiektg.generasjon_id
FROM innslag_som_enda_ikke_er_knyttet_til_generasjon iseiektg WHERE p.id = iseiektg.id;

DROP TABLE innslag_som_enda_ikke_er_knyttet_til_generasjon;
DROP TABLE nyeste_generasjon_for_periodehistorikk;
DROP INDEX IF EXISTS tmp_iseiektg_v;
DROP INDEX IF EXISTS tmp_iseiektg_u;
DROP INDEX IF EXISTS tmp_ngfp_v;
