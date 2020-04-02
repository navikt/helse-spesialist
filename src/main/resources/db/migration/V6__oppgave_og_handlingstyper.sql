ALTER TABLE oppgave DROP COLUMN type_ref;
ALTER TABLE oppgave DROP COLUMN status_ref;
DROP TABLE oppgave_type;
DROP TABLE oppgave_status;

CREATE TYPE oppgave_type AS ENUM('GodkjennPeriode');
CREATE TYPE oppgave_status AS ENUM('Fullført', 'Avventer');
ALTER TABLE oppgave ADD type oppgave_type;
ALTER TABLE oppgave ADD status oppgave_status;
DROP TABLE handling CASCADE;
DROP TABLE handling_type;
DROP TABLE handling_notat;
