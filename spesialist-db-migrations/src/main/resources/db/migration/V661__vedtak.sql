CREATE TABLE vedtak (
    behandling_id UUID NOT NULL PRIMARY KEY,
    fattet_automatisk boolean NOT NULL,
    saksbehandler_ident varchar REFERENCES saksbehandler(ident),
    beslutter_ident varchar REFERENCES saksbehandler(ident),
    tidspunkt TIMESTAMP NOT NULL
);
