update oppgave set status='Invalidert'::oppgavestatus
where id in(
    select o.id from oppgave o
    left join command_context c on o.command_context_id = c.context_id
    where o.status='AvventerSaksbehandler'::oppgavestatus AND c.id is null
);
