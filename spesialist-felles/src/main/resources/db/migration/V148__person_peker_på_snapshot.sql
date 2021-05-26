ALTER TABLE speil_snapshot ADD COLUMN person_ref INT UNIQUE REFERENCES person(id);

UPDATE speil_snapshot s SET person_ref = v.person_ref FROM (SELECT DISTINCT ON(person_ref) * FROM vedtak) v WHERE v.speil_snapshot_ref = s.id;
