create index on tildeling (oppgave_id_ref, saksbehandler_ref);
create index on oppgave (id, status, vedtak_ref) where status = 'AvventerSaksbehandler'
