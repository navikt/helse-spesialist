CREATE TABLE saksbehandler_stans_events (
    saksbehandlerstans_id UUID NOT NULL,
    sekvensnummer INT NOT NULL,
    event_navn VARCHAR(255) NOT NULL,
    utført_av_saksbehandler_ident VARCHAR(20) NOT NULL,
    tidspunkt TIMESTAMP WITH TIME ZONE NOT NULL,
    identitetsnummer VARCHAR(20) NOT NULL,
    begrunnelse TEXT NOT NULL,
    PRIMARY KEY (sekvensnummer, identitetsnummer)
)
