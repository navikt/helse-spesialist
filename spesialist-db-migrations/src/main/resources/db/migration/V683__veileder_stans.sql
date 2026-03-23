CREATE TABLE veileder_stans (
    id UUID PRIMARY KEY,
    identitetsnummer VARCHAR(11) NOT NULL,
    arsaker TEXT[] NOT NULL,
    opprettet TIMESTAMPTZ NOT NULL,
    original_melding_id UUID NOT NULL,
    opphevet_av_saksbehandler_ident VARCHAR(7),
    opphevet_begrunnelse TEXT,
    opphevet_tidspunkt TIMESTAMPTZ
);

CREATE INDEX idx_veileder_stans_identitetsnummer ON veileder_stans (identitetsnummer);
