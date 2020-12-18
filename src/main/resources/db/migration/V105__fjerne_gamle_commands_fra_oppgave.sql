UPDATE oppgave SET type = 'OpprettSaksbehandleroppgaveCommand' WHERE type = 'SaksbehandlerGodkjenningCommand';
DELETE FROM oppgave WHERE type != 'OpprettSaksbehandleroppgaveCommand';
