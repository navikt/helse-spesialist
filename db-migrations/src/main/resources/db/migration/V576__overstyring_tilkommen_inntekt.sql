CREATE TABLE overstyring_tilkommen_inntekt(
    id SERIAL PRIMARY KEY,
    overstyring_ref BIGINT REFERENCES overstyring(id) NOT NULL,
    json jsonb NOT NULL
);
