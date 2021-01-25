CREATE TABLE risikovurdering_2021
(
    id SERIAL PRIMARY KEY,
    vedtaksperiode_id UUID NOT NULL,
    kan_godkjennes_automatisk BOOLEAN NOT NULL,
    krever_supersaksbehandler BOOLEAN NOT NULL,
    data JSON not null,
    opprettet TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX ON risikovurdering_2021 (vedtaksperiode_id);
