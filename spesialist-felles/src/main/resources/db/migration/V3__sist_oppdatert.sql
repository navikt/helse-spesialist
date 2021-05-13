ALTER TABLE person ADD COLUMN enhet_ref_oppdatert DATE DEFAULT now();
ALTER TABLE person_navn ADD COLUMN oppdatert DATE DEFAULT now();
