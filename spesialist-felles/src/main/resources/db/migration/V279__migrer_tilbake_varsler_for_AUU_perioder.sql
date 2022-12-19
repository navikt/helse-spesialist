UPDATE selve_varsel sv SET
       status = 'AKTIV',
       definisjon_ref = null,
       status_endret_ident = null,
       status_endret_tidspunkt = null
FROM selve_vedtaksperiode_generasjon svg
WHERE
        svg.id = sv.generasjon_ref AND
        svg.låst = true AND
        svg.utbetaling_id IS NULL AND
        sv.status = 'GODKJENT';