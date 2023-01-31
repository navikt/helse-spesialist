with ulåste_generasjoner as (
--     Alle generasjoner som er ulåste
    select vedtaksperiode_id from selve_vedtaksperiode_generasjon svg1 where låst = false),
     har_bare_1_låst_generasjon as (
--     Alle ulåste generasjoner for vedtaksperiode som bare har 1 låst generasjon
         select vedtaksperiode_id
         from selve_vedtaksperiode_generasjon svg2
         where låst = true
           and vedtaksperiode_id in (select vedtaksperiode_id from ulåste_generasjoner)
         group by vedtaksperiode_id
         having count(vedtaksperiode_id) = 1),
     id_og_kode_på_varsler_som_ligger_feil as (
--     id og kode på aktive varsler som ligger på låst generasjon
         select sv.id, sv.kode
         from selve_vedtaksperiode_generasjon svg
                  inner join selve_varsel sv on svg.id = sv.generasjon_ref
         where svg.låst = true
           and sv.status = 'AKTIV'
           and utbetaling_id is null
           and svg.vedtaksperiode_id in (select vedtaksperiode_id from har_bare_1_låst_generasjon))
update selve_varsel sv
set generasjon_ref = (select id
                      from selve_vedtaksperiode_generasjon svg
                      where låst = false
                        and sv.vedtaksperiode_id = svg.vedtaksperiode_id
                        and vedtaksperiode_id in (select vedtaksperiode_id from har_bare_1_låst_generasjon))
from id_og_kode_på_varsler_som_ligger_feil ik
where sv.id = ik.id
  and not exists(
--         Vil ikke endre generasjon_ref på aktivt varsel hvis det samme varselet allerede finnes i den åpne generasjonen
        select 1
        from selve_varsel x
        where x.kode = sv.kode
          and x.generasjon_ref = (select id
                                  from selve_vedtaksperiode_generasjon svg
                                  where låst = false
                                    and sv.vedtaksperiode_id = svg.vedtaksperiode_id
                                    and vedtaksperiode_id in
                                        (select vedtaksperiode_id from har_bare_1_låst_generasjon)));
