-- Legger til index for oppslag for å se om overstyringen finnes ved VedtaksperiodeEndret
CREATE INDEX ON overstyring (ekstern_hendelse_id);

CREATE TABLE overstyringer_for_vedtaksperioder (
    vedtaksperiode_id UUID NOT NULL,
    overstyring_ref BIGINT NOT NULL REFERENCES overstyring(id),
    PRIMARY KEY (vedtaksperiode_id, overstyring_ref)
)