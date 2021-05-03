UPDATE oppgave o SET utbetaling_id = (h.data->>'utbetalingId')::uuid
FROM command_context ctx INNER JOIN hendelse h ON ctx.hendelse_id = h.id
WHERE ctx.context_id = o.command_context_id AND o.status in ('AvventerSystem', 'AvventerSaksbehandler');
