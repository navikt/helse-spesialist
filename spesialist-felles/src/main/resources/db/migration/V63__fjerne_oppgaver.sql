UPDATE oppgave SET status='Invalidert'::oppgavestatus WHERE status IN ('AvventerSystem'::oppgavestatus, 'AvventerSaksbehandler'::oppgavestatus)
