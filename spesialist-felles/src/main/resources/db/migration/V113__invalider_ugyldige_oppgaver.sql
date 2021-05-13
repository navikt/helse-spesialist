-- IDer hentet med:
-- select o.id from oppgave o
-- where o.status='AvventerSaksbehandler'::oppgavestatus
--  and o.opprettet < '2021-01-08 00:58:00'::timestamp


update oppgave
set status='Invalidert'::oppgavestatus
where id in (
             2341524,
             2341537,
             2341540,
             2341541,
             2341547,
             2341548,
             2341549,
             2341562,
             2341572,
             2332621,
             2332233
    );
