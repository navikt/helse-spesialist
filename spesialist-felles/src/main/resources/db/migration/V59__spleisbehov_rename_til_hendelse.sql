ALTER TABLE spleisbehov RENAME TO hendelse;
ALTER TABLE command_context RENAME COLUMN spleisbehov_id TO hendelse_id;
ALTER TABLE oppgave RENAME COLUMN event_id TO hendelse_id;
ALTER TABLE saksbehandleroppgavetype RENAME COLUMN spleisbehov_ref TO hendelse_id;
ALTER TABLE warning RENAME COLUMN spleisbehov_ref TO hendelse_id;
