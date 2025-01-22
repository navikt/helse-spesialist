update oppgave
set egenskaper = array_append(egenskaper, 'TILBAKEDATERT')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join selve_varsel sv on v.vedtaksperiode_id = sv.vedtaksperiode_id
             where sv.status in ('AKTIV', 'VURDERT')
               and kode = 'RV_SØ_3')
  and status = 'AvventerSaksbehandler'
  and not ('TILBAKEDATERT' = ANY (egenskaper));