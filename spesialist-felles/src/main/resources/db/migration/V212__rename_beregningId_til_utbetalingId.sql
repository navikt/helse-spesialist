DELETE FROM periodehistorikk;

ALTER TABLE periodehistorikk
    RENAME column beregning_id TO utbetaling_id;
