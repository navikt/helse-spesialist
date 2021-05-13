CREATE TABLE saksbehandleroppgavetype
(
    id SERIAL PRIMARY KEY,
    type VARCHAR NOT NULL,
    spleisbehov_ref UUID REFERENCES spleisbehov (id)
);
