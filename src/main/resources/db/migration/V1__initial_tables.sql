CREATE TABLE person_navn
(
    id         SERIAL,
    fornavn    VARCHAR(256) NOT NULL,
    mellomnavn VARCHAR(256),
    etternavn  VARCHAR(256) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE enhet
(
    id   INT UNIQUE NOT NULL,
    navn CHAR(256)  NOT NULL
);

CREATE TABLE person
(
    id            SERIAL PRIMARY KEY,
    fodselsnummer BIGINT NOT NULL,
    aktor_id      BIGINT NOT NULL,
    navn_ref      INT REFERENCES person_navn (id),
    enhet_ref     INT REFERENCES enhet (id)
);

CREATE TABLE person_egenskap_type
(
    id   SERIAL PRIMARY KEY,
    type CHAR(64) NOT NULL
);

CREATE TABLE person_egenskap
(
    id         SERIAL PRIMARY KEY,
    type_ref   INT NOT NULL REFERENCES person_egenskap_type (id),
    person_ref INT NOT NULL REFERENCES person (id)
);

CREATE TABLE arbeidsgiver_navn
(
    id   SERIAL PRIMARY KEY,
    navn VARCHAR(256) NOT NULL
);

CREATE TABLE arbeidsgiver
(
    id        SERIAL PRIMARY KEY,
    orgnummer INT NOT NULL,
    navn_ref  INT NOT NULL REFERENCES arbeidsgiver_navn (id)
);

CREATE TABLE speil_snapshot
(
    id   SERIAL PRIMARY KEY,
    data JSON NOT NULL
);

CREATE TABLE vedtak
(
    id                 SERIAL PRIMARY KEY,
    vedtaksperiode_id  UUID      NOT NULL,
    fom                TIMESTAMP NOT NULL,
    tom                TIMESTAMP NOT NULL,
    arbeidsgiver_ref   INT       NOT NULL REFERENCES arbeidsgiver (id),
    person_ref         INT       NOT NULL REFERENCES person (id),
    speil_snapshot_ref INT       NOT NULL REFERENCES speil_snapshot (id)
);

CREATE TABLE oppgave_type
(
    id   SERIAL PRIMARY KEY,
    type CHAR(64) NOT NULL
);

CREATE TABLE oppgave_status
(
    id     SERIAL PRIMARY KEY,
    status CHAR(64) NOT NULL
);

CREATE TABLE oppgave
(
    id         SERIAL PRIMARY KEY,
    behov_id   UUID      NOT NULL,
    opprettet  TIMESTAMP NOT NULL DEFAULT now(),
    type_ref   INT       NOT NULL REFERENCES oppgave_type (id),
    status_ref INT       NOT NULL REFERENCES oppgave_status (id)
);

CREATE TABLE handling_type
(
    id   SERIAL PRIMARY KEY,
    type CHAR(64) NOT NULL
);

CREATE TABLE handling
(
    id          SERIAL PRIMARY KEY,
    opprettet   TIMESTAMP NOT NULL DEFAULT now(),
    frist       TIMESTAMP NOT NULL,
    oppgave_ref INT       NOT NULL REFERENCES oppgave (id),
    type_ref    INT       NOT NULL REFERENCES handling_type (id)
);

CREATE TABLE handling_notat
(
    id           SERIAL PRIMARY KEY,
    handling_ref INT REFERENCES handling (id),
    notat        TEXT NOT NULL
);

CREATE TABLE person_metadata_json
(
    id   SERIAL PRIMARY KEY,
    data JSON NOT NULL
);

CREATE TABLE person_metadata
(
    id         SERIAL PRIMARY KEY,
    person_ref INT NOT NULL REFERENCES person (id),
    json_ref   INT NOT NULL REFERENCES person_metadata_json (id)
);
