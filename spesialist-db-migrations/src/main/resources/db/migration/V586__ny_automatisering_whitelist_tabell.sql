CREATE TABLE force_automatisering
(
    vedtaksperiode_id     uuid      not null unique,
    opprettet     timestamp not null default now()
);