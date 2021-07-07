DELETE
FROM tildeling
WHERE saksbehandler_ref = '3475a65a-3511-4235-b15e-57efbbf95a4f'
  AND oppgave_id_ref IN (SELECT id FROM oppgave WHERE status = 'AvventerSaksbehandler'::oppgavestatus);
