UPDATE utbetaling_id ui
SET
    arbeidsgiverbeløp = (data->'arbeidsgiverOppdrag'->>'nettoBeløp')::int,
    personbeløp = (data->'personOppdrag'->>'nettoBeløp')::int
FROM utbetaling u WHERE ui.id = u.utbetaling_id_ref;