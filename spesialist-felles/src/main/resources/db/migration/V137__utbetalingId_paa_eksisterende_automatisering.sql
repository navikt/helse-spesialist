UPDATE automatisering a
SET utbetaling_id = (h.data ->> 'utbetalingId')::uuid
FROM hendelse h
WHERE h.id = a.hendelse_ref AND (h.data ->> 'utbetalingId') IS NOT NULL;
