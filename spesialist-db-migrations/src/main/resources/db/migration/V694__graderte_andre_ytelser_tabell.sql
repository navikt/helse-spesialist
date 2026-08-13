create table graderte_andre_ytelser_events
(
    pk                            bigserial primary key,
    graderte_andre_ytelser_id     uuid      not null,
    sekvensnummer                 int       not null,
    type                          varchar   not null,
    tidspunkt                     timestamp not null,
    utført_av_saksbehandler_ident varchar   not null,
    notat_til_beslutter           varchar   not null,

    fødselsnummer                 varchar   not null,
    totrinnsvurdering_id          bigint    not null,

    data_json                     varchar   null,

    constraint graderte_andre_ytelser_unik_id unique (graderte_andre_ytelser_id, sekvensnummer)
);
