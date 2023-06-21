ALTER TABLE hendelse ALTER COLUMN type TYPE VARCHAR(64);

CREATE TABLE skjonnsfastsetting_sykepengegrunnlag (
    ID SERIAL PRIMARY KEY,
    arlig NUMERIC(12, 2) NOT NULL,
    fra_arlig NUMERIC(12, 2) DEFAULT NULL,
    skjaeringstidspunkt TIMESTAMP NOT NULL,
    arsak TEXT NOT NULL,
    begrunnelse TEXT NOT NULL,
    subsumsjon json,
    arbeidsgiver_ref INT REFERENCES arbeidsgiver(id) NOT NULL,
    overstyring_ref BIGINT REFERENCES overstyring(id) NOT NULL
);
