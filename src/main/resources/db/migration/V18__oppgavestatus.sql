ALTER TABLE oppgave RENAME COLUMN løsningstype TO status;

ALTER TABLE oppgave ADD COLUMN ferdigstilt_av VARCHAR(64);
UPDATE oppgave SET ferdigstilt_av='System' WHERE status='System' AND ferdigstilt IS NOT NULL;

ALTER TABLE oppgave ALTER COLUMN status TYPE VARCHAR(64);
DROP TYPE løsningstype_type;
CREATE TYPE oppgavestatus AS ENUM('AvventerSystem', 'AvventerSaksbehandler', 'Ferdigstilt', 'Invalidert');
ALTER TABLE oppgave ALTER COLUMN status TYPE oppgavestatus USING (status::oppgavestatus);

UPDATE oppgave SET status='Ferdigstilt'::oppgavestatus WHERE ferdigstilt IS NOT NULL;
ALTER TABLE oppgave RENAME COLUMN ferdigstilt TO oppdatert;

UPDATE oppgave SET oppdatert=now() WHERE oppdatert IS NULL;
ALTER TABLE oppgave ALTER COLUMN oppdatert SET NOT NULL;
