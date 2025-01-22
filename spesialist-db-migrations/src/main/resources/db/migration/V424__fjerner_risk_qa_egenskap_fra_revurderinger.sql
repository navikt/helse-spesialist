update oppgave
set egenskaper = array_remove(egenskaper, 'RISK_QA')
where type = 'REVURDERING'
  and ('RISK_QA' = ANY (egenskaper))
  and status = 'AvventerSaksbehandler';