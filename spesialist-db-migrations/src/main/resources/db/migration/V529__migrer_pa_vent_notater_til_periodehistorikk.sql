-- Denne brukes som arbeidstabell mens migreringen pågår.
-- Oppdaterer til slutt periodehistorikk-tabellen fra denne tabellen
create temporary table temp_notat as
select *
from notat
where type = 'PaaVent';

alter table temp_notat add column generasjon_id uuid;
alter table temp_notat add column frist date;

create index tmp_temp_notat_vedtaksperiode on temp_notat (vedtaksperiode_id);

-- Setter generasjon_id for alle notater bortsett fra de som ligger på siste generasjon
-- med hjelp av tid
with vindu as (select opprettet_tidspunkt,
                      lag(opprettet_tidspunkt, 1)
                      over (partition by svg.vedtaksperiode_id order by opprettet_tidspunkt) as forrige_opprettet,
                      lag(unik_id, 1)
                      over (partition by svg.vedtaksperiode_id order by opprettet_tidspunkt) as forrige_unik_id,
                      svg.vedtaksperiode_id
               from selve_vedtaksperiode_generasjon svg
                        inner join temp_notat tn on tn.vedtaksperiode_id = svg.vedtaksperiode_id)
update temp_notat tn
set generasjon_id = v.forrige_unik_id
from vindu v
where v.vedtaksperiode_id = tn.vedtaksperiode_id
  and (tn.opprettet between v.forrige_opprettet and v.opprettet_tidspunkt)
  and forrige_unik_id is not null
  and forrige_opprettet is not null;

-- Holder på siste generasjon for alle vedtaksperioder med på_vent notater
create temporary table nyeste_generasjon_for_notater as
select unik_id, vedtaksperiode_id
from (select unik_id,
             vedtaksperiode_id,
             row_number() over (partition by vedtaksperiode_id order by svg.opprettet_tidspunkt desc) as rn
      from selve_vedtaksperiode_generasjon svg
               inner join temp_notat tn using (vedtaksperiode_id)) x
where rn = 1;

-- Oppdaterer notat med generasjon_id, der hvor notatet er knyttet til siste generasjon av vedtaksperioden
update temp_notat tn
set generasjon_id = ngfn.unik_id
from nyeste_generasjon_for_notater ngfn
where tn.vedtaksperiode_id = ngfn.vedtaksperiode_id;

-- Holder på nyeste notat for hver generasjon
create temporary table nyeste_notat_for_generasjon as
select generasjon_id, vedtaksperiode_id, id
from (select tn.generasjon_id,
             vedtaksperiode_id,
             tn.id,
             row_number() over (partition by generasjon_id order by tn.opprettet desc) as rn
      from temp_notat tn) x
where rn = 1;

-- Oppdaterer nyeste notat pr generasjon med frist
update temp_notat tn
set frist = pv.frist
from nyeste_notat_for_generasjon nnfg
         join pa_vent pv using (vedtaksperiode_id)
where tn.id = nnfg.id;

-- Sletter periodehistorikkelementer med tom json. De vil bli insertet igjen i neste spørring.
delete
from periodehistorikk
where type = 'LEGG_PA_VENT'
  and json::text = '{}'::text;

-- Inserter på vent-innslag fra notat-arbeidstabell til periodehistorikk
insert into periodehistorikk (type, timestamp, generasjon_id, saksbehandler_oid, notat_id, json)
select 'LEGG_PA_VENT'                                                 as type,
       tn.opprettet                                                   as timestamp,
       tn.generasjon_id,
       tn.saksbehandler_oid,
       tn.id                                                          as notat_id,
       jsonb_build_object('årsaker', '{}'::json[], 'frist', tn.frist) as json
from temp_notat tn;

drop table temp_notat;
drop table nyeste_generasjon_for_notater;
drop table nyeste_notat_for_generasjon;
drop index if exists tmp_temp_notat_vedtaksperiode;