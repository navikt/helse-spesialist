create index on oppgave (oppdatert, ferdigstilt_av_oid) where status in ('Ferdigstilt', 'AvventerSystem');
create index on totrinnsvurdering (utbetaling_id_ref, saksbehandler) where utbetaling_id_ref is not null;
