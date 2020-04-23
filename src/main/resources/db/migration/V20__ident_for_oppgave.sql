ALTER TABLE oppgave ADD COLUMN ferdigstilt_av_oid UUID;
UPDATE oppgave SET ferdigstilt_av_oid = '${spesialist_oid}' WHERE ferdigstilt_av = 'System';
