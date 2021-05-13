ALTER TABLE oppgave ADD COLUMN vedtak_ref INT REFERENCES vedtak (id);
