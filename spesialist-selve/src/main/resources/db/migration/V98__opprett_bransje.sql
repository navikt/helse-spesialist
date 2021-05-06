CREATE TABLE arbeidsgiver_bransjer
(
    id        SERIAL PRIMARY KEY,
    bransjer  TEXT NOT NULL,
    oppdatert TIMESTAMP DEFAULT now()
);

ALTER TABLE arbeidsgiver ADD COLUMN bransjer_ref BIGINT REFERENCES arbeidsgiver_bransjer (id);
