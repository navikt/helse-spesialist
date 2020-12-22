ALTER TABLE oppgave DROP COLUMN type;

CREATE TYPE oppgavetype AS ENUM ('SØKNAD', 'STIKKPRØVE');

ALTER TABLE oppgave ADD COLUMN type oppgavetype DEFAULT 'SØKNAD'::oppgavetype NOT NULL;
