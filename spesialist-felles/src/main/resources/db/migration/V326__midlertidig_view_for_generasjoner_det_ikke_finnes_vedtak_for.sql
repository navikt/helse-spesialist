CREATE MATERIALIZED VIEW generasjon_mangler_vedtak AS (
SELECT DISTINCT vedtaksperiode_id FROM selve_vedtaksperiode_generasjon svg EXCEPT (SELECT vedtaksperiode_id FROM vedtak)
);

CREATE UNIQUE INDEX ON generasjon_mangler_vedtak(vedtaksperiode_id);