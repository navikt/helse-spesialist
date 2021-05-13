ALTER TABLE utbetaling
    ALTER COLUMN utbetaling_id_ref SET NOT NULL,
    ALTER COLUMN arbeidsgiver_fagsystem_id_ref SET NOT NULL,
    ALTER COLUMN person_fagsystem_id_ref SET NOT NULL,
    DROP COLUMN utbetaling_id,
    DROP COLUMN arbeidsgiver_fagsystem_id,
    DROP COLUMN person_fagsystem_id;
