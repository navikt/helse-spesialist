CREATE TABLE IF NOT EXISTS tmp_slettede_varsler
(
    LIKE selve_varsel INCLUDING ALL
);

WITH ranked AS (SELECT s.*,
                       row_number() OVER (
                           PARTITION BY unik_id, vedtaksperiode_id
                           ORDER BY id DESC
                           ) AS rn
                FROM selve_varsel s)
INSERT
INTO tmp_slettede_varsler
SELECT r.id,
       r.unik_id,
       r.kode,
       r.vedtaksperiode_id,
       r.generasjon_ref,
       r.definisjon_ref,
       r.opprettet,
       r.status,
       r.status_endret_ident,
       r.status_endret_tidspunkt
FROM ranked r
WHERE r.rn > 1;

DELETE
FROM selve_varsel s
    USING tmp_slettede_varsler t
WHERE s.id = t.id;
