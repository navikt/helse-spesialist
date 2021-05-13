ALTER TABLE oppgave RENAME COLUMN besvart TO ferdigstilt;
ALTER TABLE oppgave DROP COLUMN status;
DROP TYPE oppgave_status;
CREATE TYPE løsningstype_type AS ENUM('System', 'Saksbehandler');
ALTER TABLE oppgave ADD COLUMN løsningstype løsningstype_type;
