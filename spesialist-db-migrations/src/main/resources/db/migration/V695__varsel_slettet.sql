-- Revisjonstabell for varsler som slettes. Skrives kun til, leses ikke av applikasjonen.
create table varsel_slettet
(
    id                      bigserial primary key,
    unik_id                 uuid      not null,
    kode                    varchar   not null,
    vedtaksperiode_id       uuid      not null,
    behandling_ref          bigint    not null,
    definisjon_ref          bigint,
    opprettet               timestamp not null,
    status                  varchar   not null,
    status_endret_ident     varchar,
    status_endret_tidspunkt timestamp,
    slettet_tidspunkt       timestamp not null default now(),
    årsak                   varchar   not null
);
