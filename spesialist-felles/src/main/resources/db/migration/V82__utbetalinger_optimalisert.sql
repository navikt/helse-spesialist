CREATE TABLE utbetaling_id
(
    id            SERIAL PRIMARY KEY,
    utbetaling_id UUID UNIQUE NOT NULL
);

CREATE TABLE oppdrag
(
    id           SERIAL PRIMARY KEY,
    fagsystem_id varchar(32) unique not null
);

ALTER TABLE utbetaling
    ADD COLUMN utbetaling_id_ref BIGINT REFERENCES utbetaling_id (id),
    ADD COLUMN arbeidsgiver_fagsystem_id_ref BIGINT REFERENCES oppdrag (id),
    ADD COLUMN person_fagsystem_id_ref BIGINT REFERENCES oppdrag (id);
