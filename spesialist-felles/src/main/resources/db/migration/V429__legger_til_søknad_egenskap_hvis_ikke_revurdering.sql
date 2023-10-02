update oppgave
set egenskaper = array_append(egenskaper, 'SØKNAD')
where status = 'AvventerSaksbehandler'
  and not ('SØKNAD' = ANY (egenskaper))
  and not ('REVURDERING' = ANY (egenskaper));