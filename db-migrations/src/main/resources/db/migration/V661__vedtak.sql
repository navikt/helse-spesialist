CREATE TABLE vedtak (
    behandling_id UUID NOT NULL PRIMARY KEY,
    fattet_automatisk boolean NOT NULL,
    saksbehandler_ident varchar,
    beslutter_ident varchar,
    tidspunkt TIMESTAMP NOT NULL
);
