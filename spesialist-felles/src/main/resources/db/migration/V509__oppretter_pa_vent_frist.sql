CREATE TABLE pa_vent_frist (
    id                            SERIAL PRIMARY KEY,
    pa_vent_ref                   BIGINT REFERENCES pa_vent(id) NOT NULL,
    frist                         DATE NOT NULL,
    notat_ref                     BIGINT REFERENCES notat(id),
    opprettet                     TIMESTAMP DEFAULT now() NOT NULL
);

