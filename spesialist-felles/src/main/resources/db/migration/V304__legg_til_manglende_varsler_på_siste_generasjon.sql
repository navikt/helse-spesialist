WITH har_senere_generasjoner AS (
    SELECT generasjon_id, vedtaksperiode_id FROM temp_manglende_varsler tmv
    INTERSECT (
        SELECT unik_id, vedtaksperiode_id FROM selve_vedtaksperiode_generasjon svg
    )
    EXCEPT (
        SELECT unik_id, vedtaksperiode_id
        FROM selve_vedtaksperiode_generasjon svg1
        WHERE id = (SELECT max(id) FROM selve_vedtaksperiode_generasjon svg2 WHERE svg1.vedtaksperiode_id = svg2.vedtaksperiode_id)
    )
),
har_kun_1_senere_generasjon_og_den_er_ulåst AS (
    SELECT svg.vedtaksperiode_id FROM har_senere_generasjoner hsg
    JOIN selve_vedtaksperiode_generasjon svg ON svg.vedtaksperiode_id = hsg.vedtaksperiode_id
    WHERE svg.id > (select id from selve_vedtaksperiode_generasjon svg where svg.unik_id = hsg.generasjon_id)
        AND låst = false
    GROUP BY svg.vedtaksperiode_id HAVING count(svg.unik_id) = 1
)
INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, generasjon_ref, opprettet)
    SELECT tmv.varsel_id, tmv.varselkode, tmv.vedtaksperiode_id, svg.id, tmv.tidspunkt
    FROM temp_manglende_varsler tmv
    JOIN har_kun_1_senere_generasjon_og_den_er_ulåst hk1 ON tmv.vedtaksperiode_id = hk1.vedtaksperiode_id
    JOIN selve_vedtaksperiode_generasjon svg on hk1.vedtaksperiode_id = svg.vedtaksperiode_id
    WHERE svg.id = (SELECT max(id) FROM selve_vedtaksperiode_generasjon svg2 WHERE hk1.vedtaksperiode_id = svg2.vedtaksperiode_id)
      AND tmv.varselkode IS NOT NULL
ON CONFLICT (generasjon_ref, kode) DO NOTHING;
