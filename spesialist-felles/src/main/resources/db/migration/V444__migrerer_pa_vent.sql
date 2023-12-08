ALTER TABLE pa_vent ALTER COLUMN frist SET NOT NULL;
ALTER TABLE pa_vent ADD CONSTRAINT vedtaksperiode_id_key UNIQUE(vedtaksperiode_id);

INSERT INTO pa_vent(vedtaksperiode_id, saksbehandler_ref, frist)
SELECT v.vedtaksperiode_id, t.saksbehandler_ref, CURRENT_DATE + 14
FROM oppgave o
    INNER JOIN vedtak v
ON o.vedtak_ref = v.id
    INNER JOIN tildeling t ON o.id = t.oppgave_id_ref
WHERE o.status = 'AvventerSaksbehandler'
  AND t.på_vent = true
ON CONFLICT (vedtaksperiode_id) DO NOTHING;