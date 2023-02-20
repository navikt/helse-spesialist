SELECT vedtaksperiode_id
INTO TEMP vedtaksperioder_med_flere_ulåste_generasjoner
FROM selve_vedtaksperiode_generasjon svg
GROUP BY vedtaksperiode_id
HAVING SUM(CASE WHEN låst THEN 0 ELSE 1 END) > 1;

SELECT DISTINCT ON (svg.vedtaksperiode_id) svg.vedtaksperiode_id, unik_id, id
INTO TEMP siste_generasjoner
FROM selve_vedtaksperiode_generasjon svg
    INNER JOIN vedtaksperioder_med_flere_ulåste_generasjoner vug ON svg.vedtaksperiode_id = vug.vedtaksperiode_id
ORDER BY vedtaksperiode_id, id DESC;

SELECT id, svg.vedtaksperiode_id
INTO TEMP generasjoner_som_er_ugyldige
FROM selve_vedtaksperiode_generasjon svg
    INNER JOIN vedtaksperioder_med_flere_ulåste_generasjoner vug ON svg.vedtaksperiode_id = vug.vedtaksperiode_id
WHERE svg.låst = false AND svg.unik_id NOT IN (SELECT unik_id FROM siste_generasjoner);

with varsler_som_skal_flyttes AS (
    SELECT
        sv.id AS varsel_id,
        sv.kode,
        sv.vedtaksperiode_id,
        (SELECT id FROM siste_generasjoner sg WHERE sv.vedtaksperiode_id = sg.vedtaksperiode_id) AS ny_generasjon_ref,
        gu.id AS gammel_generasjon_ref
    FROM selve_varsel sv
        INNER JOIN generasjoner_som_er_ugyldige gu ON sv.generasjon_ref = gu.id
)

SELECT DISTINCT ON (kode, vedtaksperiode_id) varsel_id, kode, vedtaksperiode_id, ny_generasjon_ref, gammel_generasjon_ref
INTO TEMP deduplisert
FROM varsler_som_skal_flyttes;

UPDATE selve_varsel sv
SET generasjon_ref = dd.ny_generasjon_ref
FROM deduplisert dd
WHERE sv.generasjon_ref = dd.gammel_generasjon_ref
    AND sv.kode = dd.kode
    AND dd.kode NOT IN (SELECT kode FROM selve_varsel sv WHERE sv.generasjon_ref = dd.ny_generasjon_ref)
RETURNING sv.id;

DELETE FROM selve_varsel sv
WHERE sv.generasjon_ref IN (SELECT id FROM generasjoner_som_er_ugyldige)
RETURNING sv.id;

DELETE FROM selve_vedtaksperiode_generasjon svg
WHERE svg.id IN (SELECT id FROM generasjoner_som_er_ugyldige)
RETURNING svg.id;
