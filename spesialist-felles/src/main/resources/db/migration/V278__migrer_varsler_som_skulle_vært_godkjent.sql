WITH avh AS (
    SELECT DISTINCT ON (utbetaling_id) utbetaling_id, status_endret_tidspunkt, status_endret_ident FROM selve_varsel sv
    INNER JOIN selve_vedtaksperiode_generasjon s on s.id = sv.generasjon_ref
    WHERE sv.status = 'GODKJENT'
      AND utbetaling_id IS NOT NULL
      AND status_endret_ident != 'Automatisk behandlet'
      AND s.låst = true
    GROUP BY utbetaling_id, status_endret_ident, status_endret_tidspunkt
)
UPDATE selve_varsel sv SET
   status = 'GODKJENT',
   definisjon_ref = (SELECT id FROM api_varseldefinisjon av WHERE av.kode = sv.kode ORDER BY av.opprettet DESC LIMIT 1),
   status_endret_ident = (SELECT status_endret_ident FROM avh WHERE avh.utbetaling_id = svg.utbetaling_id LIMIT 1),
   status_endret_tidspunkt = (SELECT status_endret_tidspunkt FROM avh WHERE avh.utbetaling_id = svg.utbetaling_id LIMIT 1)
FROM selve_vedtaksperiode_generasjon svg
WHERE svg.id = sv.generasjon_ref
  AND svg.låst = true
  AND sv.status = 'AKTIV';