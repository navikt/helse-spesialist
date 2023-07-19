create sequence overstyrtdag_id_seq
    as integer;

create sequence person_navn_id_seq
    as integer;

create type oppdrag_endringskode as enum ('NY', 'UEND', 'ENDR');

create type oppdrag_fagområde as enum ('SPREF', 'SP');

create type oppdrag_klassekode as enum ('SPREFAG-IOP', 'SPREFAGFER-IOP', 'SPATORD', 'SPATFER');

create type oppdrag_statuskode as enum ('OPPH');

create type oppgavestatus as enum ('AvventerSystem', 'AvventerSaksbehandler', 'Ferdigstilt', 'Invalidert', 'MakstidOppnådd');

create type oppgavetype as enum ('SØKNAD', 'STIKKPRØVE', 'RISK_QA', 'REVURDERING', 'FORTROLIG_ADRESSE', 'UTBETALING_TIL_SYKMELDT', 'DELVIS_REFUSJON');

create type person_kjonn as enum ('Mann', 'Kvinne', 'Ukjent');

create type utbetaling_status as enum ('GODKJENT', 'SENDT', 'OVERFØRT', 'UTBETALING_FEILET', 'UTBETALT', 'ANNULLERT', 'FORKASTET', 'NY', 'IKKE_UTBETALT', 'IKKE_GODKJENT', 'GODKJENT_UTEN_UTBETALING');

create type utbetaling_type as enum ('UTBETALING', 'ETTERUTBETALING', 'ANNULLERING', 'FERIEPENGER', 'REVURDERING');

create type notattype as enum ('Retur', 'Generelt', 'PaaVent');

create type overstyringtype as enum ('Inntekt', 'Dager', 'Arbeidsforhold');

create type mottakertype as enum ('SYKMELDT', 'ARBEIDSGIVER', 'BEGGE');

create table if not exists arbeidsgiver_bransjer
(
    id        serial
        primary key,
    bransjer  text not null,
    oppdatert timestamp default now()
);

create table if not exists arbeidsgiver_navn
(
    id             serial
        primary key,
    navn           varchar(256)       not null,
    navn_oppdatert date default now() not null
);

create table if not exists arbeidsgiver
(
    id           serial
        primary key,
    orgnummer    bigint not null
        unique
        constraint arbeidsgiver_orgnummer_key1
            unique
        constraint arbeidsgiver_orgnummer_key2
            unique,
    navn_ref     integer
        references arbeidsgiver_navn,
    bransjer_ref bigint
        references arbeidsgiver_bransjer
);

create table if not exists enhet
(
    id   integer      not null
        unique,
    navn varchar(256) not null
);

create table if not exists feilende_meldinger
(
    id         uuid                    not null
        primary key,
    event_name varchar(40)             not null,
    opprettet  timestamp default now() not null,
    blob       json                    not null
);

create table if not exists global_snapshot_versjon
(
    id          integer                 not null
        primary key,
    versjon     integer                 not null,
    sist_endret timestamp default now() not null
);

create table if not exists hendelse
(
    id            uuid        not null
        primary key,
    data          json        not null,
    type          varchar(64) not null,
    fodselsnummer bigint      not null
);

create table if not exists command_context
(
    id          serial
        primary key,
    context_id  uuid                    not null,
    hendelse_id uuid                    not null
        references hendelse
            on delete cascade,
    opprettet   timestamp default now() not null,
    tilstand    varchar(64)             not null,
    data        json                    not null
);

create index if not exists command_context_context_id_idx
    on command_context (context_id);

create index if not exists command_context_hendelse_id_idx
    on command_context (hendelse_id);

create index if not exists hendelse_fodselsnummer_idx
    on hendelse (fodselsnummer);

create table if not exists infotrygdutbetalinger
(
    id   serial
        primary key,
    data json not null
);

create table if not exists oppdrag
(
    id                   serial
        primary key,
    fagsystem_id         varchar(32) not null,
    mottaker             varchar(32),
    fagområde            "oppdrag_fagområde",
    endringskode         oppdrag_endringskode,
    sistearbeidsgiverdag date
);

create index if not exists oppdrag_fagsystem_id_idx
    on oppdrag (fagsystem_id);

create table if not exists person_info
(
    id                 integer default nextval('person_navn_id_seq'::regclass) not null
        constraint person_navn_pkey
            primary key,
    fornavn            varchar(256)                                            not null,
    mellomnavn         varchar(256),
    etternavn          varchar(256)                                            not null,
    fodselsdato        date,
    kjonn              person_kjonn,
    adressebeskyttelse varchar(32)                                             not null
);

alter sequence person_navn_id_seq owned by person_info.id;

create table if not exists person
(
    id                              serial
        primary key,
    fodselsnummer                   bigint not null
        constraint fodselsnummer_unique
            unique,
    aktor_id                        bigint not null,
    info_ref                        integer
        constraint person_navn_ref_fkey
            references person_info,
    enhet_ref                       integer
        references enhet (id),
    enhet_ref_oppdatert             date,
    personinfo_oppdatert            date,
    infotrygdutbetalinger_ref       integer
        references infotrygdutbetalinger,
    infotrygdutbetalinger_oppdatert date
);

create table if not exists arbeidsforhold
(
    id               serial
        primary key,
    person_ref       bigint                  not null
        references person,
    arbeidsgiver_ref bigint                  not null
        references arbeidsgiver,
    startdato        date                    not null,
    sluttdato        date,
    stillingstittel  text                    not null,
    stillingsprosent integer                 not null,
    oppdatert        timestamp default now() not null
);

create table if not exists egen_ansatt
(
    person_ref     bigint    not null
        primary key
        references person,
    er_egen_ansatt boolean   not null,
    opprettet      timestamp not null
);

create table if not exists gosysoppgaver
(
    person_ref     bigint    not null
        primary key
        references person,
    antall         integer,
    oppslag_feilet boolean   not null,
    opprettet      timestamp not null
);

create table if not exists opptegnelse
(
    person_id     bigint not null
        references person,
    sekvensnummer serial,
    payload       json   not null,
    type          varchar(64),
    primary key (person_id, sekvensnummer)
);

create index if not exists opptegnelse_sekvensnummer_idx
    on opptegnelse (sekvensnummer);

create index if not exists person_fodselsnummer_idx
    on person (fodselsnummer);

create table if not exists risikovurdering
(
    id                serial
        primary key,
    vedtaksperiode_id uuid      not null,
    samlet_score      integer   not null,
    ufullstendig      boolean   not null,
    opprettet         timestamp not null
);

create table if not exists risikovurdering_2021
(
    id                        serial
        primary key,
    vedtaksperiode_id         uuid                    not null,
    kan_godkjennes_automatisk boolean                 not null,
    krever_supersaksbehandler boolean                 not null,
    data                      json                    not null,
    opprettet                 timestamp default now() not null
);

create index if not exists risikovurdering_2021_vedtaksperiode_id_idx
    on risikovurdering_2021 (vedtaksperiode_id);

create table if not exists risikovurdering_arbeidsuforhetvurdering
(
    id                  serial
        primary key,
    risikovurdering_ref integer
        constraint risikovurdering_arbeidsuforhetvurderin_risikovurdering_ref_fkey
            references risikovurdering,
    tekst               text not null
);

create table if not exists risikovurdering_faresignal
(
    id                  serial
        primary key,
    risikovurdering_ref integer
        references risikovurdering,
    tekst               text not null
);

create table if not exists saksbehandler
(
    oid   uuid not null
        primary key,
    navn  varchar(64),
    epost varchar(128),
    ident varchar(64) default NULL::character varying
);

create table if not exists abonnement_for_opptegnelse
(
    saksbehandler_id    uuid   not null
        references saksbehandler,
    person_id           bigint not null
        references person,
    siste_sekvensnummer integer,
    primary key (saksbehandler_id, person_id)
);

create index if not exists abonnement_for_opptegnelse_saksbehandler_id_idx
    on abonnement_for_opptegnelse (saksbehandler_id);

create index if not exists abonnement_for_opptegnelse_saksbehandler_id_person_id_idx
    on abonnement_for_opptegnelse (saksbehandler_id, person_id);

create table if not exists annullert_av_saksbehandler
(
    id                  serial
        primary key,
    annullert_tidspunkt timestamp not null,
    saksbehandler_ref   uuid      not null
        references saksbehandler
);

create table if not exists overstyring
(
    id                  serial
        primary key,
    tidspunkt           timestamp with time zone default now() not null,
    person_ref          integer                                not null
        references person,
    hendelse_ref        uuid                                   not null
        references hendelse,
    saksbehandler_ref   uuid                                   not null
        references saksbehandler,
    ekstern_hendelse_id uuid,
    ferdigstilt         boolean                  default false
);

create index if not exists overstyring_ekstern_hendelse_id_idx
    on overstyring (ekstern_hendelse_id);

create table if not exists overstyring_arbeidsforhold
(
    id                  serial
        primary key,
    forklaring          text    not null,
    deaktivert          boolean not null,
    skjaeringstidspunkt date,
    overstyring_ref     bigint  not null
        references overstyring,
    arbeidsgiver_ref    integer not null
        references arbeidsgiver,
    begrunnelse         text    not null
);

create table if not exists overstyring_inntekt
(
    id                        serial
        primary key,
    manedlig_inntekt          numeric(12, 2)                        not null,
    skjaeringstidspunkt       date,
    forklaring                text default 'ingen forklaring'::text not null,
    overstyring_ref           bigint                                not null
        references overstyring,
    fra_manedlig_inntekt      numeric(12, 2),
    refusjonsopplysninger     json,
    fra_refusjonsopplysninger json,
    arbeidsgiver_ref          integer                               not null
        references arbeidsgiver,
    begrunnelse               text                                  not null,
    subsumsjon                json
);

create table if not exists reserver_person
(
    saksbehandler_ref  uuid                                             not null
        references saksbehandler,
    person_ref         bigint                                           not null
        unique
        references person,
    gyldig_til         timestamp default (now() + '72:00:00'::interval) not null,
    sett_på_vent_flagg boolean   default false
);

create table if not exists snapshot
(
    id         serial
        primary key,
    data       json    not null,
    versjon    integer not null,
    person_ref integer
        unique
        references person
);

create table if not exists utbetaling_id
(
    id                            serial
        primary key,
    utbetaling_id                 uuid            not null
        unique,
    person_ref                    integer         not null
        references person,
    arbeidsgiver_ref              integer         not null
        references arbeidsgiver,
    arbeidsgiver_fagsystem_id_ref bigint          not null
        references oppdrag,
    person_fagsystem_id_ref       bigint          not null
        references oppdrag,
    type                          utbetaling_type not null,
    opprettet                     timestamp       not null,
    arbeidsgiverbeløp             integer         not null,
    personbeløp                   integer         not null
);

create table if not exists utbetaling
(
    id                             serial
        primary key,
    status                         utbetaling_status not null,
    opprettet                      timestamp         not null,
    data                           json              not null,
    utbetaling_id_ref              bigint            not null
        references utbetaling_id,
    annullert_av_saksbehandler_ref bigint
        references annullert_av_saksbehandler,
    constraint status_opprettet_utbetaling_id_ref_unique
        unique (status, opprettet, utbetaling_id_ref)
);

create index if not exists utbetaling_utbetaling_id_ref_idx
    on utbetaling (utbetaling_id_ref);

create index if not exists utbetaling_id_person_ref_idx
    on utbetaling_id (person_ref);

create index if not exists utbetaling_id_arbeidsgiver_fagsystem_id_ref_idx
    on utbetaling_id (arbeidsgiver_fagsystem_id_ref);

create index if not exists utbetaling_id_person_fagsystem_id_ref_idx
    on utbetaling_id (person_fagsystem_id_ref);

create table if not exists utbetalingslinje
(
    id             serial
        primary key,
    oppdrag_id     bigint               not null
        references oppdrag,
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
    grad           integer              not null,
    totalbeløp     integer
);

create index if not exists utbetalingslinje_oppdrag_id_idx
    on utbetalingslinje (oppdrag_id);

create table if not exists vedtak
(
    id                    serial
        primary key,
    vedtaksperiode_id     uuid      not null
        unique,
    fom                   timestamp not null,
    tom                   timestamp not null,
    arbeidsgiver_ref      integer   not null
        references arbeidsgiver,
    person_ref            integer   not null
        references person,
    snapshot_ref          integer
        references snapshot,
    forkastet             boolean   not null,
    forkastet_tidspunkt   timestamp,
    forkastet_av_hendelse uuid
);

create table if not exists automatisering
(
    vedtaksperiode_ref integer                 not null
        references vedtak,
    hendelse_ref       uuid                    not null
        references hendelse,
    automatisert       boolean,
    stikkprøve         boolean   default false not null,
    opprettet          timestamp default now() not null,
    utbetaling_id      uuid,
    id                 serial
        primary key,
    inaktiv_fra        timestamp
);

create index if not exists automatisering_hendelse_ref_idx
    on automatisering (hendelse_ref);

create table if not exists automatisering_problem
(
    id                 serial
        primary key,
    vedtaksperiode_ref integer      not null
        references vedtak,
    hendelse_ref       uuid         not null
        references hendelse,
    problem            varchar(100) not null,
    inaktiv_fra        timestamp
);

create index if not exists automatisering_problem_hendelse_ref_idx
    on automatisering_problem (hendelse_ref);

create table if not exists notat
(
    id                       serial
        primary key,
    tekst                    text,
    opprettet                timestamp default now(),
    saksbehandler_oid        uuid
        constraint notat_saksbehandler_ref_fkey
            references saksbehandler,
    vedtaksperiode_id        uuid                                    not null
        constraint notat_vedtak_ref_fkey
            references vedtak (vedtaksperiode_id),
    feilregistrert           boolean   default false                 not null,
    feilregistrert_tidspunkt timestamp,
    type                     notattype default 'Generelt'::notattype not null
);

create table if not exists oppgave
(
    id                 bigserial
        primary key,
    opprettet          timestamp   default now()                 not null,
    oppdatert          timestamp                                 not null,
    status             oppgavestatus,
    vedtak_ref         integer
        references vedtak,
    ferdigstilt_av     varchar(64),
    ferdigstilt_av_oid uuid,
    command_context_id uuid,
    type               oppgavetype default 'SØKNAD'::oppgavetype not null,
    utbetaling_id      uuid,
    sist_sendt         timestamp,
    mottaker           mottakertype
);

create index if not exists oppgave_type_idx
    on oppgave (type);

create index if not exists oppgave_vedtak_ref_idx
    on oppgave (vedtak_ref);

create index if not exists oppgave_utbetaling_id_idx
    on oppgave (utbetaling_id);

create index if not exists oppgave_status_idx
    on oppgave (status)
    where (status = 'AvventerSaksbehandler'::oppgavestatus);

create table if not exists saksbehandleroppgavetype
(
    id            serial
        primary key,
    type          varchar     not null,
    vedtak_ref    bigint      not null
        unique
        references vedtak
            on delete cascade,
    inntektskilde varchar(64) not null
);

create table if not exists tildeling
(
    saksbehandler_ref uuid   not null
        references saksbehandler,
    oppgave_id_ref    bigint not null
        constraint tildeling_oppgave_ref_fkey
            references oppgave,
    på_vent           boolean default false
);

create index if not exists tildeling_oppgave_id_ref_idx
    on tildeling (oppgave_id_ref);

create index if not exists tildeling_oppgave_id_ref_saksbehandler_ref_idx
    on tildeling (oppgave_id_ref, saksbehandler_ref);

create index if not exists vedtak_person_ref_idx
    on vedtak (person_ref);

create table if not exists vedtaksperiode_hendelse
(
    hendelse_ref      uuid not null
        references hendelse
            on delete cascade,
    vedtaksperiode_id uuid not null
);

create index if not exists vedtaksperiode_hendelse_hendelse_ref_idx
    on vedtaksperiode_hendelse (hendelse_ref);

create index if not exists vedtaksperiode_hendelse_vedtaksperiode_id_idx
    on vedtaksperiode_hendelse (vedtaksperiode_id);

create table if not exists vedtaksperiode_utbetaling_id
(
    vedtaksperiode_id uuid not null,
    utbetaling_id     uuid not null,
    primary key (vedtaksperiode_id, utbetaling_id)
);

create table if not exists vergemal
(
    person_ref             bigint    not null
        primary key
        references person,
    har_vergemal           boolean   not null,
    har_fremtidsfullmakter boolean   not null,
    har_fullmakter         boolean   not null,
    opprettet              timestamp not null
);

create table if not exists periodehistorikk
(
    id                serial
        primary key,
    type              text                    not null,
    timestamp         timestamp default now() not null,
    utbetaling_id     uuid                    not null,
    saksbehandler_oid uuid
        references saksbehandler,
    notat_id          integer
        references notat
);

create table if not exists overstyringer_for_vedtaksperioder
(
    vedtaksperiode_id uuid   not null,
    overstyring_ref   bigint not null
        references overstyring,
    primary key (vedtaksperiode_id, overstyring_ref)
);

create table if not exists kommentarer
(
    id                       serial
        primary key,
    tekst                    text                    not null,
    notat_ref                integer
        references notat,
    feilregistrert_tidspunkt timestamp,
    opprettet                timestamp default now() not null,
    saksbehandlerident       varchar(64)             not null
);

create table if not exists api_varseldefinisjon
(
    id         bigserial
        primary key,
    unik_id    uuid                  not null
        unique,
    kode       varchar               not null,
    tittel     varchar               not null,
    forklaring varchar,
    handling   varchar,
    avviklet   varchar default false not null,
    opprettet  timestamp             not null
);

create index if not exists api_varseldefinisjon_kode_idx
    on api_varseldefinisjon (kode);

create table if not exists selve_vedtaksperiode_generasjon
(
    id                          bigserial
        primary key,
    unik_id                     uuid      default gen_random_uuid() not null
        unique,
    vedtaksperiode_id           uuid                                not null,
    utbetaling_id               uuid,
    opprettet_tidspunkt         timestamp default now(),
    opprettet_av_hendelse       uuid                                not null,
    tilstand_endret_tidspunkt   timestamp,
    tilstand_endret_av_hendelse uuid,
    fom                         date,
    tom                         date,
    skjæringstidspunkt          date,
    tilstand                    varchar                             not null,
    constraint selve_vedtaksperiode_generasj_vedtaksperiode_id_utbetaling__key
        unique (vedtaksperiode_id, utbetaling_id)
);

create index if not exists selve_vedtaksperiode_generasjon_utbetaling_id_idx
    on selve_vedtaksperiode_generasjon (utbetaling_id);

create index if not exists selve_vedtaksperiode_generasj_vedtaksperiode_id_skjæringst_idx
    on selve_vedtaksperiode_generasjon (vedtaksperiode_id) include (skjæringstidspunkt);

create index if not exists skjæringstidspunkt_med_utbetaling_id
    on selve_vedtaksperiode_generasjon (skjæringstidspunkt) include (utbetaling_id);

create table if not exists selve_varsel
(
    id                      bigserial
        primary key,
    unik_id                 uuid                                       not null,
    kode                    varchar                                    not null,
    vedtaksperiode_id       uuid                                       not null,
    generasjon_ref          bigint                                     not null
        references selve_vedtaksperiode_generasjon,
    definisjon_ref          bigint
        references api_varseldefinisjon,
    opprettet               timestamp                                  not null,
    status                  varchar default 'AKTIV'::character varying not null,
    status_endret_ident     varchar,
    status_endret_tidspunkt timestamp,
    unique (generasjon_ref, kode)
);

create index if not exists selve_varsel_unik_id_idx
    on selve_varsel (unik_id);

create index if not exists selve_varsel_vedtaksperiode_id_status_idx
    on selve_varsel (vedtaksperiode_id, status);

create table if not exists inntekt
(
    id                  bigserial
        primary key,
    person_ref          bigint not null
        references person,
    skjaeringstidspunkt date   not null,
    inntekter           json   not null,
    unique (person_ref, skjaeringstidspunkt)
);

create table if not exists overstyring_tidslinje
(
    id               serial
        primary key,
    overstyring_ref  integer
        references overstyring,
    arbeidsgiver_ref integer
        references arbeidsgiver,
    begrunnelse      text
);

create table if not exists overstyring_dag
(
    id                        integer default nextval('overstyrtdag_id_seq'::regclass) not null
        constraint overstyrtdag_pkey
            primary key,
    dato                      date                                                     not null,
    dagtype                   varchar(64)                                              not null,
    grad                      integer,
    fra_dagtype               varchar(64),
    fra_grad                  integer,
    overstyring_tidslinje_ref integer
        references overstyring_tidslinje
);

alter sequence overstyrtdag_id_seq owned by overstyring_dag.id;

create table if not exists totrinnsvurdering
(
    id                serial
        primary key,
    vedtaksperiode_id uuid                    not null,
    er_retur          boolean   default false not null,
    saksbehandler     uuid
        references saksbehandler,
    beslutter         uuid
        references saksbehandler,
    utbetaling_id_ref integer
        references utbetaling_id,
    opprettet         timestamp default now() not null,
    oppdatert         timestamp
);

create table if not exists opprinnelig_soknadsdato
(
    vedtaksperiode_id uuid      not null
        primary key,
    soknad_mottatt    timestamp not null
);

create table if not exists automatisering_korrigert_soknad
(
    vedtaksperiode_id uuid not null,
    hendelse_ref      uuid not null,
    constraint automatisering_korrigert_sokn_vedtaksperiode_id_hendelse_re_key
        unique (vedtaksperiode_id, hendelse_ref)
);

create index if not exists automatisering_korrigert_soknad_vedtaksperiode_id_idx
    on automatisering_korrigert_soknad (vedtaksperiode_id);

create index if not exists automatisering_korrigert_soknad_hendelse_ref_idx
    on automatisering_korrigert_soknad (hendelse_ref);

create table if not exists oppgave_behandling_kobling
(
    oppgave_id    bigint not null,
    behandling_id uuid   not null,
    primary key (oppgave_id, behandling_id)
);

create table if not exists skjonnsfastsetting_sykepengegrunnlag
(
    id                            serial
        primary key,
    arlig                         numeric(12, 2) not null,
    fra_arlig                     numeric(12, 2) default NULL::numeric,
    skjaeringstidspunkt           timestamp      not null,
    arsak                         text           not null,
    subsumsjon                    json,
    arbeidsgiver_ref              integer        not null
        references arbeidsgiver,
    overstyring_ref               bigint         not null
        references overstyring,
    initierende_vedtaksperiode_id uuid,
    begrunnelse_ref               bigint         not null
);

create table if not exists begrunnelse
(
    id                bigserial
        primary key,
    tekst             text not null,
    type              varchar,
    saksbehandler_ref uuid not null
);

create table if not exists generasjon_begrunnelse_kobling
(
    generasjon_id  uuid   not null,
    begrunnelse_id bigint not null,
    primary key (generasjon_id, begrunnelse_id)
);

create function oppdater_oppdatert_kolonne() returns trigger
    language plpgsql
as
$$
BEGIN
    NEW.oppdatert
        = now();
    RETURN NEW;
END;
$$;

create trigger oppdater_arbeidsforhold_oppdatert
    before update
    on arbeidsforhold
    for each row
execute procedure oppdater_oppdatert_kolonne();

create trigger oppdater_arbeidsgiver_bransjer_oppdatert
    before update
    on arbeidsgiver_bransjer
    for each row
execute procedure oppdater_oppdatert_kolonne();

create trigger oppdater_oppgave_oppdatert
    before update
    on oppgave
    for each row
execute procedure oppdater_oppdatert_kolonne();

