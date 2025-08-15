-- Endringen er å fjerne kolonnen kan_avvises fra include - den er ikke lenger en del av spørringen (og ble uansett ikke
-- brukt av koden

drop index oppgave_status_idx;

create index oppgave_status_idx on oppgave (status)
    include (id, egenskaper, opprettet, vedtak_ref)
    where status = 'AvventerSaksbehandler'::oppgavestatus;
