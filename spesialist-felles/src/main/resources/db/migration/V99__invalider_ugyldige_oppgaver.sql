update oppgave
set status='Invalidert'::oppgavestatus
where id in (
    select distinct id
    from oppgave o
             left join automatisering a on a.vedtaksperiode_ref = o.vedtak_ref
    where o.status = 'AvventerSaksbehandler'::oppgavestatus
      AND a.automatisert = true
);
