DROP TABLE graderte_andre_ytelser_events;

CREATE TABLE graderte_andre_ytelser_events
(
    versjon                       uuid        PRIMARY KEY,
    fødselsnummer                 varchar     NOT NULL,
    yrkesaktivitetstype           varchar     NOT NULL,
    organisasjonsnummer           varchar     NULL,
    tidspunkt                     timestamptz NOT NULL,
    utført_av_saksbehandler_ident varchar     NOT NULL,
    notat_til_beslutter           varchar     NOT NULL,
    data_json                     jsonb       NOT NULL
);

CREATE INDEX graderte_andre_ytelser_events_stream_idx
    ON graderte_andre_ytelser_events (fødselsnummer, yrkesaktivitetstype, organisasjonsnummer, versjon);
