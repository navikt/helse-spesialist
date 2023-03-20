with perioder_med_lovme as (select vedtaksperiode_id
                            from selve_vedtaksperiode_generasjon svg
                            where låst = false
                              and exists(select *
                                         from selve_varsel
                                         where kode = 'RV_MV_1'
                                           and svg.id = generasjon_ref)),
     uberørte_oppgaver as (select o.id, v.person_ref
                           from oppgave o,
                                vedtak v,
                                perioder_med_lovme p
                           where o.er_totrinnsoppgave = true
                             and o.er_beslutteroppgave = false
                             and o.er_returoppgave = false
                             and o.status = 'AvventerSaksbehandler'
                             and o.vedtak_ref = v.id
                             and v.vedtaksperiode_id = p.vedtaksperiode_id
                             and not exists(select *
                                            from overstyringer_for_vedtaksperioder ofv
                                            where ofv.vedtaksperiode_id = v.vedtaksperiode_id))
update oppgave
set er_totrinnsoppgave = false
where id in (select id from uberørte_oppgaver);
