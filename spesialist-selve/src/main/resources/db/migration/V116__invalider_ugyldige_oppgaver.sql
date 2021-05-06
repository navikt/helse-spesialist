-- IDer hentet med:
-- select o.id from oppgave o
-- where o.status='AvventerSaksbehandler'::oppgavestatus
--  and o.opprettet < '2021-01-08 00:58:00'::timestamp


update oppgave
set status='Invalidert'::oppgavestatus
where id = 2342164;
