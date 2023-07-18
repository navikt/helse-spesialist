CREATE TABLE generasjon_begrunnelse_kobling (
    generasjon_id uuid NOT NULL,
    begrunnelse_id BIGINT NOT NULL,
    PRIMARY KEY (generasjon_id, begrunnelse_id)
);