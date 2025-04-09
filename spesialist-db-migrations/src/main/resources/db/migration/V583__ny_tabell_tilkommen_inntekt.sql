create table tilkommen_inntekt
(
    uuid                       uuid primary key,
    fødselsnummer              varchar   not null,
    sekvensnummer              int       not null,
    tidspunkt                  timestamp not null,
    utførtAvSaksbehandlerIdent varchar   not null,
    notatTilBeslutter          varchar   not null,
    totrinnsvurderingId        bigint    not null,
    type                       varchar   not null,
    json                       varchar   null
);
