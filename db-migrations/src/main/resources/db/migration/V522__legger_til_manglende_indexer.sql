CREATE INDEX ON overstyring_dag(overstyring_tidslinje_ref);

CREATE INDEX ON person(aktor_id);

DROP INDEX IF EXISTS person_fodselsnummer_idx