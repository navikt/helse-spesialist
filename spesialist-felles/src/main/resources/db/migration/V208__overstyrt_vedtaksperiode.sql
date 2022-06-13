CREATE TABLE overstyrt_vedtaksperiode
(
    id SERIAL PRIMARY KEY,
    vedtaksperiode_id UUID NOT NULL
);

CREATE INDEX ON overstyrt_vedtaksperiode (vedtaksperiode_id);
