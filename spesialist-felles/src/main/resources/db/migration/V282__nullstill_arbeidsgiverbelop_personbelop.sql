ALTER TABLE utbetaling_id DROP COLUMN arbeidsgiverbeløp;
ALTER TABLE utbetaling_id DROP COLUMN personbeløp;

ALTER TABLE utbetaling_id ADD COLUMN arbeidsgiverbeløp INTEGER;
ALTER TABLE utbetaling_id ADD COLUMN personbeløp INTEGER;