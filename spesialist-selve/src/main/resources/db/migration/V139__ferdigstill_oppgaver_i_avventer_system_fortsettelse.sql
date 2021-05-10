UPDATE oppgave SET status = 'Ferdigstilt' WHERE id in (
    SELECT o.id oppgaveId FROM oppgave o
    INNER JOIN utbetaling_id ui ON ui.utbetaling_id = o.utbetaling_id
    INNER JOIN (
        select distinct on (utbetaling_id_ref) utbetaling_id_ref, opprettet, status
        from utbetaling
        order by utbetaling_id_ref, opprettet desc
    ) u ON u.utbetaling_id_ref = ui.id
    WHERE o.status = 'AvventerSystem' AND u.opprettet <= timestamp '2021-05-10 10:00:00'
);
