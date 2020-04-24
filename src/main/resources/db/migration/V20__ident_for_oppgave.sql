ALTER TABLE oppgave ALTER COLUMN oppdatert TYPE timestamp USING oppdatert::timestamp;

UPDATE oppgave as a SET status = 'Ferdigstilt', ferdigstilt_av = 'System', oppdatert = now() WHERE type != 'SaksbehandlerGodkjenningCommand'
                            AND status != 'Ferdigstilt'
                             AND (SELECT count(*) FROM oppgave AS b
                                  WHERE b.behov_id = a.behov_id AND type = 'SaksbehandlerGodkjenningCommand') > 0;

ALTER TABLE oppgave ADD COLUMN ferdigstilt_av_oid UUID;
UPDATE oppgave SET ferdigstilt_av_oid = '${spesialist_oid}' WHERE ferdigstilt_av = 'System';

UPDATE oppgave SET ferdigstilt_av = 'Spesialist' WHERE ferdigstilt_av = 'System';
