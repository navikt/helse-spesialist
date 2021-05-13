create type oppdrag_endringskode as enum ('NY', 'UEND', 'ENDR');
create type oppdrag_fagområde as enum ('SPREF', 'SP');
create type oppdrag_klassekode as enum ('SPREFAG-IOP');
create type oppdrag_statuskode as enum ('OPPH');

alter table oppdrag drop constraint oppdrag_fagsystem_id_key;
alter table oppdrag
    add column mottaker             varchar(32),
    add column fagområde            oppdrag_fagområde,
    add column endringskode         oppdrag_endringskode,
    add column sisteArbeidsgiverdag date;

create TYPE oppdrag_type AS
(
    "fagsystemId"             varchar(32),
    mottaker             varchar(32),
    fagområde           oppdrag_fagområde,
    endringskode         oppdrag_endringskode,
    "sisteArbeidsgiverdag" date
);
