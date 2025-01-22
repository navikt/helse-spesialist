update oppgave
set egenskaper = array_append(egenskaper, 'FORSTEGANGSBEHANDLING')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join saksbehandleroppgavetype s on v.id = s.vedtak_ref
             where s.type = 'FØRSTEGANGSBEHANDLING')
  and status = 'AvventerSaksbehandler'
  and not ('FORSTEGANGSBEHANDLING' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'FORLENGELSE')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join saksbehandleroppgavetype s on v.id = s.vedtak_ref
             where s.type = 'FORLENGELSE')
  and status = 'AvventerSaksbehandler'
  and not ('FORLENGELSE' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'INFOTRYGDFORLENGELSE')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join saksbehandleroppgavetype s on v.id = s.vedtak_ref
             where s.type = 'INFOTRYGDFORLENGELSE')
  and status = 'AvventerSaksbehandler'
  and not ('INFOTRYGDFORLENGELSE' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'OVERGANG_FRA_IT')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join saksbehandleroppgavetype s on v.id = s.vedtak_ref
             where s.type = 'OVERGANG_FRA_IT')
  and status = 'AvventerSaksbehandler'
  and not ('OVERGANG_FRA_IT' = ANY (egenskaper));