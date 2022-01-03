CREATE TABLE snapshot (
  id   SERIAL PRIMARY KEY,
  data JSON NOT NULL,
  versjon INT NOT NULL,
  person_ref INT UNIQUE REFERENCES person(id)
);

ALTER TABLE vedtak ADD COLUMN snapshot_ref INT REFERENCES snapshot(id) default null;
