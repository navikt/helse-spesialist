DELETE
FROM tildeling
WHERE ctid IN (SELECT t.ctid
               FROM tildeling t
                        JOIN oppgave o ON t.oppgave_id_ref = o.id
               WHERE t.saksbehandler_ref = '799d9d5a-d36e-4e82-adc8-83fd1a6ac2a1'
                 AND o.status = 'AvventerSaksbehandler');