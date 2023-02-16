WITH siste_generasjoner AS (
    SELECT generasjon_id, vedtaksperiode_id FROM temp_manglende_varsler tmv
    INTERSECT (
        SELECT unik_id, vedtaksperiode_id
        FROM selve_vedtaksperiode_generasjon svg1
        WHERE id = (SELECT max(id) FROM selve_vedtaksperiode_generasjon svg2 WHERE svg1.vedtaksperiode_id = svg2.vedtaksperiode_id)
    )
),
siste_generasjoner_som_ikke_er_utbetalt AS (
    SELECT svg.vedtaksperiode_id, svg.unik_id FROM siste_generasjoner sg
    JOIN selve_vedtaksperiode_generasjon svg ON svg.unik_id = sg.generasjon_id
    WHERE svg.utbetaling_id IS NULL
),
nye_generasjoner AS (
    INSERT INTO selve_vedtaksperiode_generasjon (vedtaksperiode_id, opprettet_av_hendelse)
        SELECT sgu.vedtaksperiode_id, '00000000-0000-0000-0000-000000000000'
        FROM siste_generasjoner_som_ikke_er_utbetalt sgu
        RETURNING vedtaksperiode_id, id
)
INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, generasjon_ref, opprettet)
    SELECT tmv.varsel_id, tmv.varselkode, tmv.vedtaksperiode_id, ng.id, tmv.tidspunkt
    FROM temp_manglende_varsler tmv
    JOIN nye_generasjoner ng ON ng.vedtaksperiode_id = tmv.vedtaksperiode_id
    JOIN siste_generasjoner_som_ikke_er_utbetalt sgu ON sgu.unik_id = tmv.generasjon_id
    ON CONFLICT (generasjon_ref, kode) DO NOTHING;