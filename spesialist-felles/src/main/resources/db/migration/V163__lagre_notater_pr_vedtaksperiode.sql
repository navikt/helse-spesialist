DROP TABLE notat;

CREATE TABLE notat
(
    id                 SERIAL PRIMARY KEY,
    tekst              VARCHAR(200),
    opprettet          TIMESTAMP DEFAULT now(),
    saksbehandler_oid  UUID,
    saksbehandler_navn VARCHAR(100),
    vedtaksperiode_id  UUID NOT NULL
        CONSTRAINT notat_vedtak_ref_fkey REFERENCES vedtak (vedtaksperiode_id)
);
