ALTER TABLE totrinnsvurdering
    DROP CONSTRAINT totrinnsvurdering_utbetalt_utbetaling_ref_fkey;

ALTER TABLE totrinnsvurdering
    RENAME COLUMN utbetalt_utbetaling_ref TO utbetaling_id_ref;

ALTER TABLE totrinnsvurdering
    ADD CONSTRAINT totrinnsvurdering_utbetaling_id_ref_fkey FOREIGN KEY (utbetaling_id_ref) REFERENCES utbetaling_id (id);