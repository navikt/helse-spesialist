create table stans_automatisk_behandling_saksbehandler
(
    fødselsnummer varchar primary key
        references person (fødselsnummer),
    opprettet timestamp not null
);
