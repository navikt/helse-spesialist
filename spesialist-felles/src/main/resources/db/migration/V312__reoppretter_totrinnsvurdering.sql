DROP TABLE totrinnsvurdering;

CREATE TABLE totrinnsvurdering
(
    id                      SERIAL PRIMARY KEY,
    vedtaksperiode_id       UUID      NOT NULL,
    er_retur                BOOLEAN   NOT NULL DEFAULT false,
    saksbehandler           UUID references saksbehandler (oid),
    beslutter               UUID references saksbehandler (oid),
    utbetalt_utbetaling_ref INT references utbetaling (id),
    opprettet               TIMESTAMP NOT NULL DEFAULT now(),
    oppdatert               TIMESTAMP
);
