CREATE TABLE pa_vent (
    id                            SERIAL PRIMARY KEY,
    vedtaksperiode_id             UUID NOT NULL,
    saksbehandler_ref             UUID NOT NULL,
    frist                         DATE,
    begrunnelse                   VARCHAR,
    opprettet                     TIMESTAMP DEFAULT now() NOT NULL
);