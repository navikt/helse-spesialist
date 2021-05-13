INSERT INTO utbetaling_id AS uid (utbetaling_id)
    SELECT u.utbetaling_id FROM utbetaling u
ON CONFLICT (utbetaling_id) DO NOTHING;

INSERT INTO oppdrag AS o (fagsystem_id)
    SELECT u.arbeidsgiver_fagsystem_id FROM utbetaling u
ON CONFLICT (fagsystem_id) DO NOTHING;

INSERT INTO oppdrag AS o (fagsystem_id)
    SELECT u.person_fagsystem_id FROM utbetaling u
ON CONFLICT (fagsystem_id) DO NOTHING;
