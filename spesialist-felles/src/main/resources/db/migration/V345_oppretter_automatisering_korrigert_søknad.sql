CREATE TABLE automatisering_korrigert_soknad (
    vedtaksperiode_id uuid NOT NULL,
    hendelse_ref uuid NOT NULL,
    UNIQUE (vedtaksperiode_id, hendelse_ref)
);

CREATE INDEX ON automatisering_korrigert_soknad(vedtaksperiode_id);