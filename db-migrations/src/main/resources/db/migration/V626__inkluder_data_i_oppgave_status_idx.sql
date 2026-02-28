drop index oppgave_status_idx;

create index oppgave_status_idx on oppgave (status)
    include (id, egenskaper, opprettet, kan_avvises, vedtak_ref)
    where status = 'AvventerSaksbehandler'::oppgavestatus;
