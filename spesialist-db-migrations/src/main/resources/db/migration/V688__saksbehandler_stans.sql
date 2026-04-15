DROP TABLE IF EXISTS saksbehandler_stans_events;

CREATE TABLE saksbehandler_stans
(
    id                   UUID PRIMARY KEY,
    identitetsnummer     VARCHAR(11) NOT NULL,
    utfort_av_ident      VARCHAR(20) NOT NULL,
    begrunnelse          TEXT        NOT NULL,
    opprettet            TIMESTAMPTZ NOT NULL,
    opphevet_av_ident    VARCHAR(20),
    opphevet_begrunnelse TEXT,
    opphevet_tidspunkt   TIMESTAMPTZ
);

CREATE INDEX idx_saksbehandler_stans_identitetsnummer ON saksbehandler_stans (identitetsnummer);