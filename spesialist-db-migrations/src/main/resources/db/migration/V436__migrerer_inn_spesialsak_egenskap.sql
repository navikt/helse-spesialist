update oppgave
set egenskaper = array_append(egenskaper, 'SPESIALSAK')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join spesialsak s on v.vedtaksperiode_id = s.vedtaksperiode_id
             )
  and status = 'AvventerSaksbehandler'
  and not ('SPESIALSAK' = ANY (egenskaper));