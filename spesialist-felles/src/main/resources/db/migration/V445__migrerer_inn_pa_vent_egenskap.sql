update oppgave
set egenskaper = array_append(egenskaper, 'PÅ_VENT')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join pa_vent pv on v.vedtaksperiode_id = pv.vedtaksperiode_id
             )
  and status = 'AvventerSaksbehandler'
  and not ('PÅ_VENT' = ANY (egenskaper));