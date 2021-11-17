CREATE TABLE vedtaksperiode_utbetaling_id
(
    vedtaksperiode_id UUID NOT NULL,
    utbetaling_id     UUID NOT NULL,
    PRIMARY KEY (vedtaksperiode_id, utbetaling_id)
);
