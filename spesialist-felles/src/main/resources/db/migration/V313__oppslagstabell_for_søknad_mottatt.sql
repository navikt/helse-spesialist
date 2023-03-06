create table opprinnelig_soknadsdato
(
    vedtaksperiode_id uuid primary key,
    soknad_mottatt    timestamp not null
);

insert into opprinnelig_soknadsdato
select vedtaksperiode_id, min(opprettet_tidspunkt)
from selve_vedtaksperiode_generasjon
group by vedtaksperiode_id
