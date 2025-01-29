CREATE TABLE behandling_v2(
    vedtaksperiode_id uuid not null,
    behandling_id uuid not null primary key,
    fom timestamp not null,
    tom timestamp not null,
    skjæringstidspunkt timestamp not null,
    opprettet timestamp not null
);

CREATE INDEX ON behandling_v2(vedtaksperiode_id);
