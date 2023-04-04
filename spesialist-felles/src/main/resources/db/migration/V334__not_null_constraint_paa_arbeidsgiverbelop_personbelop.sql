-- NULL-verdier i disse to kolonnene eksisterte kun for rader av type FERIEPENGER og opprettet frem til 2021-11-16 05:46:47.325000. Da var det
-- samtidig ingen rader med verdi=0 så null ble tilsynelatende brukt konsekvent (for FERIEPENGER). Skulle man ønske å revertere denne oppdateringen kan en dermed
-- sette kolonneverdiene tilbake til null der verdien er 0, type = FERIEPENGER og opprettet <= 2021-11-16 05:46:47.325000.
UPDATE utbetaling_id SET arbeidsgiverbeløp=0 WHERE arbeidsgiverbeløp IS NULL AND type='FERIEPENGER';
UPDATE utbetaling_id SET personbeløp=0 WHERE personbeløp IS NULL AND type='FERIEPENGER';

ALTER TABLE utbetaling_id ALTER COLUMN arbeidsgiverbeløp SET NOT NULL;
ALTER TABLE utbetaling_id ALTER COLUMN personbeløp SET NOT NULL;