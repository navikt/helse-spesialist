-- IDer hentet med:
-- select *, p.aktor_id from oppgave o
-- inner join vedtak v on o.vedtak_ref = v.id
-- inner join person p on v.person_ref = p.id
-- where o.status='AvventerSaksbehandler'::oppgavestatus
--  and o.opprettet < '2021-02-01 00:58:00'::timestamp

update oppgave
set status='Invalidert'::oppgavestatus
where id in (
             2342207,
             2342330,
             2342672,
             2342691
    );
