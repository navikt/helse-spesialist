create table tilkommen_inntekt_events
(
    pk                            bigserial primary key,
    tilkommen_inntekt_id          uuid      not null,
    sekvensnummer                 int       not null,
    type                          varchar   not null,
    tidspunkt                     timestamp not null,
    utført_av_saksbehandler_ident varchar   not null,
    notat_til_beslutter           varchar   not null,

    fødselsnummer                 varchar   not null,
    totrinnsvurdering_id          bigint    not null,

    data_json                     varchar   null,

    constraint unique_event_id unique (tilkommen_inntekt_id, sekvensnummer)
);
