UPDATE utbetaling u
    SET utbetaling_id_ref = uid.id
FROM utbetaling_id uid
    WHERE uid.utbetaling_id = u.utbetaling_id;

UPDATE utbetaling u
    SET arbeidsgiver_fagsystem_id_ref = o.id
FROM oppdrag o
    WHERE o.fagsystem_id = u.arbeidsgiver_fagsystem_id;

UPDATE utbetaling u
    SET person_fagsystem_id_ref = o.id
FROM oppdrag o
    WHERE o.fagsystem_id = u.person_fagsystem_id;
