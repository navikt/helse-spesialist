update oppgave
set egenskaper = array_append(egenskaper, 'UTLAND')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join person p on v.person_ref = p.id
             where p.enhet_ref in ('0393', '2101'))
  and status = 'AvventerSaksbehandler'
  and not ('UTLAND' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'EN_ARBEIDSGIVER')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join saksbehandleroppgavetype s on v.id = s.vedtak_ref
             where s.inntektskilde = 'EN_ARBEIDSGIVER')
  and status = 'AvventerSaksbehandler'
  and not ('EN_ARBEIDSGIVER' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'FLERE_ARBEIDSGIVERE')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join saksbehandleroppgavetype s on v.id = s.vedtak_ref
             where s.inntektskilde = 'FLERE_ARBEIDSGIVERE'
)
  and status = 'AvventerSaksbehandler'
  and not ('FLERE_ARBEIDSGIVERE' = ANY (egenskaper));