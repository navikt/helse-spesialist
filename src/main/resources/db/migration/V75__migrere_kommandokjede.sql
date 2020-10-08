UPDATE command_context c1 SET tilstand='FERDIG'
WHERE c1.tilstand='SUSPENDERT' AND c1.id IN (
    SELECT DISTINCT ON(c2.context_id) c2.id
    FROM command_context c2
    WHERE c2.context_id IN (
        SELECT o.command_context_id FROM oppgave o
        WHERE o.status IN ('AvventerSaksbehandler'::oppgavestatus, 'AvventerSystem'::oppgavestatus)
    )
    ORDER BY c2.context_id, c2.id DESC
);
