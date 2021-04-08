ALTER TABLE automatisering ADD COLUMN opprettet timestamp;

UPDATE automatisering a SET opprettet = (h.data->>'@opprettet')::timestamp
    FROM hendelse h
WHERE a.hendelse_ref = h.id;
