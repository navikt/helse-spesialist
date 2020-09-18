CREATE TABLE vedtaksperiode_hendelse (
    vedtaksperiode_ref INT NOT NULL REFERENCES vedtak (id) ON DELETE CASCADE,
    hendelse_ref uuid NOT NULL REFERENCES spleisbehov (id),
    PRIMARY KEY (vedtaksperiode_ref, hendelse_ref)
);

ALTER TABLE command_context
DROP COLUMN vedtaksperiode_id;

