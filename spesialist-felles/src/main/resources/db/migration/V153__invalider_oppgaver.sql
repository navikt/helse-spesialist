/*
ID-er hentet med:

SELECT *
FROM oppgave o1
WHERE (SELECT COUNT(*)
       FROM oppgave o2
       WHERE o1.vedtak_ref = o2.vedtak_ref
         AND o2.status = 'Ferdigstilt'
         AND o2.opprettet > '2021-02-01 00:00:00'
      ) > 0
  AND o1.status = 'AvventerSaksbehandler'
  AND o1.opprettet > '2021-02-01 00:00:00';
*/

UPDATE oppgave
SET status='Invalidert'::oppgavestatus
WHERE id IN (
             2435445,
             2435488
    )
