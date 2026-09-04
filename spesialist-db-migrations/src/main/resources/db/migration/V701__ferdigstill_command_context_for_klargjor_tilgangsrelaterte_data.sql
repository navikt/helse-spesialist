WITH siste_tilstand AS (
    SELECT DISTINCT ON (context_id) context_id, hendelse_id, tilstand
    FROM command_context
    WHERE hendelse_id IN (SELECT id FROM hendelse WHERE type = 'KLARGJØR_TILGANGSRELATERTE_DATA')
    ORDER BY context_id, id DESC
)
INSERT INTO command_context (context_id, hendelse_id, opprettet, tilstand, data, hash)
SELECT context_id, hendelse_id, now(), 'FERDIG', '{"sti":[]}'::json, NULL
FROM siste_tilstand
WHERE tilstand IN ('SUSPENDERT', 'FEIL');
