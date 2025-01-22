update oppgave
set egenskaper = array_append(egenskaper, 'BESLUTTER')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join totrinnsvurdering t on v.vedtaksperiode_id = t.vedtaksperiode_id
             where t.saksbehandler is not null
               and t.er_retur = false
               and t.utbetaling_id_ref is null)
  and status = 'AvventerSaksbehandler'
  and not ('BESLUTTER' = any (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'RETUR')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join totrinnsvurdering t on v.vedtaksperiode_id = t.vedtaksperiode_id
             where t.er_retur = true
               and t.utbetaling_id_ref is null)
  and status = 'AvventerSaksbehandler'
  and not ('RETUR' = any (egenskaper));