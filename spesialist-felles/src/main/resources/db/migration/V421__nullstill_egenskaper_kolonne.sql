ALTER TABLE oppgave DROP COLUMN egenskaper;
ALTER TABLE oppgave ADD COLUMN egenskaper VARCHAR[] NOT NULL DEFAULT ARRAY[]::VARCHAR[];