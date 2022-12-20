UPDATE selve_vedtaksperiode_generasjon svg
SET utbetaling_id = NULL
FROM utbetaling_id ui INNER JOIN utbetaling u on ui.id = u.utbetaling_id_ref
WHERE svg.utbetaling_id = ui.utbetaling_id AND u.status = 'FORKASTET';