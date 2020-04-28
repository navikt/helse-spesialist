UPDATE oppgave SET status='Invalidert'::oppgavestatus WHERE status='AvventerSaksbehandler'::oppgavestatus AND opprettet < '2020-04-28 15:15:00'::timestamp;
