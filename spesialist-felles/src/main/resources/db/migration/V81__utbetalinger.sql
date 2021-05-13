CREATE TYPE utbetaling_type AS ENUM ('UTBETALING', 'ETTERUTBETALING', 'ANNULLERING');
CREATE TYPE utbetaling_status AS ENUM ('GODKJENT', 'SENDT', 'OVERFØRT', 'UTBETALING_FEILET', 'UTBETALT', 'ANNULLERT');
CREATE TABLE utbetaling
(
    id                        SERIAL PRIMARY KEY,
    utbetaling_id             UUID              NOT NULL,
    person_ref                INT               NOT NULL REFERENCES person (id),
    arbeidsgiver_ref          INT               NOT NULL REFERENCES arbeidsgiver (id),
    type                      utbetaling_type   not null,
    status                    utbetaling_status not null,
    opprettet                 TIMESTAMP         NOT NULL,
    arbeidsgiver_fagsystem_id varchar(32)       not null,
    person_fagsystem_id       varchar(32)       not null,
    data                      JSON              NOT NULL
);
