INSERT INTO command_context(context_id, hendelse_id, tilstand, data)
SELECT context_id, hendelse_id, 'AVBRUTT', data
FROM (
         SELECT DISTINCT ON (context_id) *
         FROM command_context
         WHERE hendelse_id IN (
             SELECT id
             FROM hendelse
             WHERE type = 'GODKJENNING'
         )
         ORDER BY context_id, id DESC
     ) AS command_contexts
WHERE tilstand IN ('NY', 'SUSPENDERT');
