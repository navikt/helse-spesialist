UPDATE oppdrag SET fagsystem_id=regexp_replace(fagsystem_id, E'[\\r\\n]+', '', 'g' ) WHERE fagsystem_id LIKE E'%\r\n';

UPDATE utbetalingslinje SET reffagsystemid=regexp_replace(reffagsystemid, E'[\\r\\n]+', '', 'g' ) WHERE reffagsystemid LIKE E'%\r\n';
