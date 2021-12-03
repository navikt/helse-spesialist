CREATE TABLE snapshot (
  id   SERIAL PRIMARY KEY,
  data JSON NOT NULL,
  versjon INT NOT NULL,
  person_ref INT UNIQUE REFERENCES person(id)
)
