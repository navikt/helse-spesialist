update oppgave
set status = 'Invalidert', oppdatert=now(), ferdigstilt_av='manuelt invalidert'
where id in (2798225, 2798348)
