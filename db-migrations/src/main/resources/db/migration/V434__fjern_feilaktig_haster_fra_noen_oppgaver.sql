WITH skal_ikke_fjernes AS ( -- Fjerner ikke HASTER fra oppgaver med totrinnsvurdering, siden disse kanskje har vært overstyrt
    SELECT DISTINCT ON (v.vedtaksperiode_id) o.id FROM oppgave o
        INNER JOIN utbetaling_id ui on o.utbetaling_id = ui.utbetaling_id
        INNER JOIN vedtak v on v.id = o.vedtak_ref
        INNER JOIN totrinnsvurdering t on t.vedtaksperiode_id = v.vedtaksperiode_id AND utbetaling_id_ref IS NULL
    WHERE o.status = 'AvventerSaksbehandler'
        AND egenskaper && ARRAY['HASTER']::varchar[] AND personbeløp = 0
), fjernet AS (
    SELECT o.id, array_remove(egenskaper, 'HASTER') as egenskaper FROM oppgave o
        INNER JOIN utbetaling_id ui on o.utbetaling_id = ui.utbetaling_id
    WHERE status = 'AvventerSaksbehandler'
        AND egenskaper && ARRAY['HASTER']::varchar[]
        AND personbeløp = 0
        AND NOT EXISTS (SELECT 1 FROM skal_ikke_fjernes WHERE o.id = skal_ikke_fjernes.id)
)
UPDATE oppgave o
SET egenskaper = fjernet.egenskaper
FROM fjernet
WHERE o.id = fjernet.id;