ALTER TABLE tildeling ADD COLUMN på_vent BOOLEAN DEFAULT false;

ALTER TABLE oppgave_makstid DROP COLUMN tildelt;
