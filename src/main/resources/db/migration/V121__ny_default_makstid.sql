ALTER TABLE oppgave_makstid ALTER COLUMN makstid SET DEFAULT date_trunc('day', now()) + (24*60*60 - 1) * interval '1 second' + INTERVAL '7 DAYS';
