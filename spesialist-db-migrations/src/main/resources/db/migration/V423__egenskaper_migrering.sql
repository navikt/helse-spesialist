update oppgave
set egenskaper = array_append(egenskaper, 'EGEN_ANSATT')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join person p on v.person_ref = p.id
                      inner join egen_ansatt ea on p.id = ea.person_ref
             where ea.er_egen_ansatt = true)
  and status = 'AvventerSaksbehandler'
  and not ('EGEN_ANSATT' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'FORTROLIG_ADRESSE')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join person p on v.person_ref = p.id
                      inner join person_info pi on p.info_ref = pi.id
             where pi.adressebeskyttelse = 'Fortrolig')
  and status = 'AvventerSaksbehandler'
  and not ('FORTROLIG_ADRESSE' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'REVURDERING')
where id in (select o.id
             from oppgave o
                      inner join utbetaling_id ui on o.utbetaling_id = ui.utbetaling_id
             where ui.type = 'REVURDERING')
  and status = 'AvventerSaksbehandler'
  and not ('REVURDERING' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'STIKKPRØVE')
where type = 'STIKKPRØVE'
  and status = 'AvventerSaksbehandler'
  and not ('STIKKPRØVE' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'RISK_QA')
where type = 'RISK_QA'
  and status = 'AvventerSaksbehandler'
  and not ('RISK_QA' = ANY (egenskaper))
  and not ('REVURDERING' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'VERGEMÅL')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join person p on v.person_ref = p.id
                      inner join vergemal vm on p.id = vm.person_ref
             where har_vergemal = true)
  and status = 'AvventerSaksbehandler'
  and not ('VERGEMÅL' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'DELVIS_REFUSJON')
where id in (select o.id
             from oppgave o
                      inner join utbetaling_id ui on o.utbetaling_id = ui.utbetaling_id
             where (ui.personbeløp != 0 and ui.arbeidsgiverbeløp != 0))
  and status = 'AvventerSaksbehandler'
  and not ('DELVIS_REFUSJON' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'UTBETALING_TIL_SYKMELDT')
where id in (select o.id
             from oppgave o
                      inner join utbetaling_id ui on o.utbetaling_id = ui.utbetaling_id
             where ui.personbeløp != 0)
  and status = 'AvventerSaksbehandler'
  and not ('UTBETALING_TIL_SYKMELDT' = ANY (egenskaper))
  and not ('DELVIS_REFUSJON' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'UTBETALING_TIL_ARBEIDSGIVER')
where id in (select o.id
             from oppgave o
                      inner join utbetaling_id ui on o.utbetaling_id = ui.utbetaling_id
             where ui.arbeidsgiverbeløp != 0)
  and status = 'AvventerSaksbehandler'
  and not ('UTBETALING_TIL_ARBEIDSGIVER' = ANY (egenskaper))
  and not ('DELVIS_REFUSJON' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'INGEN_UTBETALING')
where id in (select o.id
             from oppgave o
                      inner join utbetaling_id ui on o.utbetaling_id = ui.utbetaling_id
             where (ui.personbeløp = 0 and ui.arbeidsgiverbeløp = 0))
  and status = 'AvventerSaksbehandler'
  and not ('INGEN_UTBETALING' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'HASTER')
where id in (select o.id
             from oppgave o
                      inner join vedtak v on o.vedtak_ref = v.id
                      inner join selve_varsel sv on v.vedtaksperiode_id = sv.vedtaksperiode_id
             where sv.status in ('AKTIV', 'VURDERT')
               and kode = 'RV_UT_23')
  and status = 'AvventerSaksbehandler'
  and not ('HASTER' = ANY (egenskaper));

update oppgave
set egenskaper = array_append(egenskaper, 'SØKNAD')
where status = 'AvventerSaksbehandler'
  and not ('SØKNAD' = ANY (egenskaper))
  and not ('REVURDERING' = ANY (egenskaper))



