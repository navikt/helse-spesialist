drop table utbetaling;
drop table utbetalingslinje;
drop table utbetaling_id;
drop table oppdrag;

drop type oppdrag_endringskode;
drop type oppdrag_fagområde;
drop type oppdrag_klassekode;
drop type oppdrag_statuskode;

create type oppdrag_endringskode as enum ('NY', 'UEND', 'ENDR');
create type oppdrag_fagområde as enum ('SPREF', 'SP');
create type oppdrag_klassekode as enum ('SPREFAG-IOP');
create type oppdrag_statuskode as enum ('OPPH');

create table oppdrag
(
    id                   serial      not null constraint oppdrag_pkey primary key,
    fagsystem_id         varchar(32) not null,
    mottaker             varchar(32),
    fagområde            oppdrag_fagområde,
    endringskode         oppdrag_endringskode,
    sistearbeidsgiverdag date
);

create table utbetalingslinje
(
    id             serial               not null constraint utbetalingslinje_pkey primary key,
    oppdrag_id     bigint               not null constraint utbetalingslinje_oppdrag_id_fkey references oppdrag,
    delytelseid    integer              not null,
    refdelytelseid integer,
    reffagsystemid varchar(32),
    endringskode   oppdrag_endringskode not null,
    klassekode     oppdrag_klassekode   not null,
    statuskode     oppdrag_statuskode,
    datostatusfom  date,
    fom            date                 not null,
    tom            date                 not null,
    dagsats        integer              not null,
    lønn           integer              not null,
    grad           integer              not null
);

create table utbetaling_id
(
    id                            serial          not null constraint utbetaling_id_pkey primary key,
    utbetaling_id                 uuid            not null constraint utbetaling_id_utbetaling_id_key unique,
    person_ref                    integer         not null constraint utbetaling_id_person_ref_fkey references person,
    arbeidsgiver_ref              integer         not null constraint utbetaling_id_arbeidsgiver_ref_fkey references arbeidsgiver,
    arbeidsgiver_fagsystem_id_ref bigint          not null constraint utbetaling_id_arbeidsgiver_fagsystem_id_ref_fkey references oppdrag,
    person_fagsystem_id_ref       bigint          not null constraint utbetaling_id_person_fagsystem_id_ref_fkey references oppdrag,
    type                          utbetaling_type not null,
    opprettet                     timestamp       not null
);

create table utbetaling
(
    id                serial            not null constraint utbetaling_pkey primary key,
    status            utbetaling_status not null,
    opprettet         timestamp         not null,
    data              json              not null,
    utbetaling_id_ref bigint            not null constraint utbetaling_utbetaling_id_ref_fkey references utbetaling_id
);

