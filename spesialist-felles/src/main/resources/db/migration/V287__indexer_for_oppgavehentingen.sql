drop index oppgave_id_status_vedtak_ref_idx;
create index oppgave_status_vedtak_ref_type_er_beslutteroppgave_idx on oppgave (status, type, er_beslutteroppgave) where status = 'AvventerSaksbehandler'
