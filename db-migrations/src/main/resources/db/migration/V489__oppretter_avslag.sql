CREATE table avslag (
    vedtaksperiode_id UUID NOT NULL,
    begrunnelse_ref BIGINT NOT NULL REFERENCES begrunnelse(id),
    generasjon_ref BIGINT NOT NULL REFERENCES selve_vedtaksperiode_generasjon(id)
);