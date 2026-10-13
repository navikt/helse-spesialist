CREATE TABLE saksbehandlerhandling
(
    id                   UUID PRIMARY KEY,
    identitetsnummer     VARCHAR(11) NOT NULL,
    handling_id          UUID        NOT NULL UNIQUE,
    handling_type        TEXT        NOT NULL,
    saksbehandler_ident  VARCHAR(20) NOT NULL,
    tidspunkt            TIMESTAMPTZ NOT NULL,
    totrinnsvurdering_id BIGINT REFERENCES totrinnsvurdering (id),
    skjaeringstidspunkt  DATE,
    behandling_id        UUID
);

CREATE INDEX idx_saksbehandlerhandling_identitetsnummer ON saksbehandlerhandling (identitetsnummer);
CREATE INDEX idx_saksbehandlerhandling_totrinnsvurdering_id ON saksbehandlerhandling (totrinnsvurdering_id);
