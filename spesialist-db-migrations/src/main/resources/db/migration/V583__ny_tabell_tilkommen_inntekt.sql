create table tilkommen_inntekt
(
    pk                         bigserial primary key,
    tilkommenInntektId         uuid      not null,
    fødselsnummer              varchar   not null,
    sekvensnummer              int       not null,
    tidspunkt                  timestamp not null,
    utførtAvSaksbehandlerIdent varchar   not null,
    notatTilBeslutter          varchar   not null,
    totrinnsvurderingId        bigint    not null,
    type                       varchar   not null,
    json                       varchar   null,

    constraint unique_event_id unique (tilkommenInntektId, sekvensnummer)
);
