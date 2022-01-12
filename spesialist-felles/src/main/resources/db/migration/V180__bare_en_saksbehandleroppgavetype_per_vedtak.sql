-- Fjern utdaterte rader som peker på samme vedtak som en nyere rad:
DELETE
FROM saksbehandleroppgavetype
WHERE id IN
      (SELECT sub.id
       FROM (SELECT s.id,
                    rank() OVER (
                        PARTITION BY s.vedtak_ref
                        ORDER BY id desc
                        ) rangering
             FROM saksbehandleroppgavetype s
            ) AS sub
       WHERE rangering > 1
      );

ALTER TABLE saksbehandleroppgavetype ADD unique(vedtak_ref);
