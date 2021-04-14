/*

ID-er hentet med:

SELECT *
FROM oppgave o
  JOIN vedtak v ON o.vedtak_ref = v.id
  JOIN person p ON v.person_ref = p.id
  JOIN hendelse h ON h.fodselsnummer = p.fodselsnummer
WHERE o.status='AvventerSaksbehandler'::oppgavestatus
  AND h.type = 'VEDTAKSPERIODE_ENDRET'
  AND h.fodselsnummer = p.fodselsnummer
  AND v.vedtaksperiode_id = CAST(h.data->>'vedtaksperiodeId' AS uuid)
  AND h.data->>'gjeldendeTilstand' = 'TIL_INFOTRYGD'

 */

UPDATE oppgave
SET status='Invalidert'::oppgavestatus
WHERE id IN (2383445, 2387818)
