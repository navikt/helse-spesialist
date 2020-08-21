UPDATE oppgave
SET (status, ferdigstilt_av, ferdigstilt_av_oid, oppdatert) = ('AvventerSaksbehandler'::oppgavestatus, NULL, NULL, now())
WHERE event_id in (
             '0aebd990-39b2-4434-9729-e77cecee6c8c',
             '1dcc2102-21f6-40e3-b1e9-01c63ff05a60',
             '2f91a7a9-02d7-4b9e-9dad-a0d4dd86f9db',
             '482ea929-b5da-42a5-ac0f-9d1d88b7f4f2',
             '54ff4634-eafc-4a5f-a315-92e9c4f36db6',
             '14016eee-b0ae-4683-ba42-bde550c1080c',
             'ce857338-95dd-4b7f-b57a-1755ccc110aa',
             '80e7bb50-e282-42ba-937d-5fd1e4e3228a',
             '35d67621-5eb9-477e-bdbd-31eec968803d',
             'feaecad0-111f-4794-bb56-f2c6af8da54d',
             '54e55b15-a15e-478a-811d-6bd5c46fb9f1',
             'e3417673-17ad-48a4-b8c2-d817e172046b',
             'e70e9269-dfde-48c4-b18f-e87a1274117f',
             '15918a60-e933-4548-b425-74b68554abd4'
    )
  AND type = 'SaksbehandlerGodkjenningCommand'
  AND status = 'Ferdigstilt'::oppgavestatus
  AND oppdatert < '2020-08-21 14:50:00'::timestamp
