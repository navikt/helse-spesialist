create table stans_automatisering
(
    id               serial primary key,
    fødselsnummer    varchar(11) not null,
    status           varchar(64) not null,
    årsaker          varchar[]   not null,
    tidsstempel      timestamp   not null,
    kilde            varchar(64) not null,
    original_melding json        not null
);
