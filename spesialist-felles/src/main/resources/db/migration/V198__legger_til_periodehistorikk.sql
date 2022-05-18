CREATE TABLE periodehistorikk
(
    id                SERIAL PRIMARY KEY,
    type              TEXT NOT NULL,
    timestamp         TIMESTAMP NOT NULL DEFAULT now(),
    periode_id        UUID NOT NULL,
    saksbehandler_oid UUID NOT NULL REFERENCES saksbehandler (oid),
    notat_id          INT REFERENCES notat (id)
);