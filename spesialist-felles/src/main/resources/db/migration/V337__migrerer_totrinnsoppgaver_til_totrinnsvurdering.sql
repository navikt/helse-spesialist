INSERT INTO totrinnsvurdering (vedtaksperiode_id, er_retur, saksbehandler, beslutter)
SELECT v.vedtaksperiode_id, o.er_returoppgave, t.saksbehandler_ref, o.beslutter_saksbehandler_oid
FROM oppgave o
         INNER JOIN vedtak v ON v.id = o.vedtak_ref
         LEFT JOIN totrinnsvurdering ttv
                   ON (ttv.vedtaksperiode_id = v.vedtaksperiode_id AND ttv.utbetaling_id_ref IS null)
         LEFT JOIN person p on v.person_ref = p.id
         LEFT JOIN tildeling t ON o.id = t.oppgave_id_ref
WHERE o.status = 'AvventerSaksbehandler'
  AND o.er_totrinnsoppgave = true
  AND ttv.vedtaksperiode_id IS null;