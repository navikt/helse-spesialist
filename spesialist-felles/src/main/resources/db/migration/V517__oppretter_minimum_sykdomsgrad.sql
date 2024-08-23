CREATE TABLE overstyring_minimum_sykdomsgrad (
    id SERIAL PRIMARY KEY NOT NULL,
    overstyring_ref BIGINT REFERENCES overstyring(id),
    fom DATE NOT NULL,
    tom DATE NOT NULL,
    vurdering BOOLEAN NOT NULL,
    begrunnelse TEXT NOT NULL
);

CREATE TABLE overstyring_minimum_sykdomsgrad_arbeidsgiver (
    id SERIAL PRIMARY KEY NOT NULL,
    berort_vedtaksperiode_id UUID NOT NULL,
    arbeidsgiver_ref INTEGER NOT NULL REFERENCES arbeidsgiver (id),
    overstyring_minimum_sykdomsgrad_ref BIGINT REFERENCES overstyring_minimum_sykdomsgrad (id)
);