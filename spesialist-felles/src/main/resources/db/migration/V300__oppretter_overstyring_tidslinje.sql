CREATE TABLE overstyring_tidslinje (
       id                  SERIAL PRIMARY KEY,
       overstyring_ref     INT REFERENCES overstyring (id),
       arbeidsgiver_ref    INT REFERENCES arbeidsgiver (id),
       begrunnelse         TEXT
);

ALTER TABLE overstyring_dag
    ADD COLUMN overstyring_tidslinje_ref INT REFERENCES overstyring_tidslinje (id);