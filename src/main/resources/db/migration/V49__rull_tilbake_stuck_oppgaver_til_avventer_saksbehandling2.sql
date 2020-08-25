UPDATE oppgave
SET (status, ferdigstilt_av, ferdigstilt_av_oid, oppdatert) = ('AvventerSaksbehandler'::oppgavestatus, NULL, NULL, now())
WHERE event_id in (
    '3d2ed0db-e8b4-4613-a460-a9a965dd389e',
    '312e63b3-bbd9-475f-8118-7362a890e0f6',
    'f355acd2-ae01-4c66-9f73-77cf75923eb6',
    '3d9fdaf2-b1c8-4176-9f21-f2551491e987'
    )
  AND type = 'SaksbehandlerGodkjenningCommand'
  AND status = 'Ferdigstilt'::oppgavestatus
  AND oppdatert < '2020-08-25 13:38:00'::timestamp
