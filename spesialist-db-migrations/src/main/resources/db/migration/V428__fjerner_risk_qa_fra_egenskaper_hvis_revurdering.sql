update oppgave
set egenskaper = array_append(egenskaper, 'SØKNAD')
where type = 'REVURDERING'
  and ('RISK_QA' = ANY (egenskaper))
  and status = 'AvventerSaksbehandler';