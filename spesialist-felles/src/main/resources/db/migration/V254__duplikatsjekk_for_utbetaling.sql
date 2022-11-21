-- Henter ut alle duplikater med samme status, opprettet og utbetaling_id
-- Sletter den med høyest id
DELETE FROM utbetaling u1 USING utbetaling u2 WHERE u1.id > u2.id AND u1.status = u2.status AND u1.opprettet = u2.opprettet AND u1.utbetaling_id_ref = u2.utbetaling_id_ref;

ALTER TABLE utbetaling ADD CONSTRAINT status_opprettet_utbetaling_id_ref_unique UNIQUE (status, opprettet, utbetaling_id_ref);