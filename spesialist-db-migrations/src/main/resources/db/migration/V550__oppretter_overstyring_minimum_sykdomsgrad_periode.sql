CREATE TABLE overstyring_minimum_sykdomsgrad_periode (
    id SERIAL NOT NULL,
    fom DATE NOT NULL,
    tom DATE NOT NULL,
    vurdering BOOLEAN NOT NULL,
    overstyring_minimum_sykdomsgrad_ref BIGINT NOT NULL REFERENCES overstyring_minimum_sykdomsgrad(id)
);

INSERT INTO overstyring_minimum_sykdomsgrad_periode (fom, tom, vurdering, overstyring_minimum_sykdomsgrad_ref)
SELECT fom, tom, vurdering, id FROM overstyring_minimum_sykdomsgrad;