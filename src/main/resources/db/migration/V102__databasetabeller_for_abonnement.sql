CREATE TABLE abonnement_for_oppdatering
(
    saksbehandler_id         UUID NOT NULL, -- REFERENCES saksbehandler(oid),
    person_id                bigint NOT NULL REFERENCES person(id),
    siste_sekvensnummer      integer,
    primary key(saksbehandler_id, person_id)
);

CREATE TABLE opptegning
(
    person_id                bigint NOT NULL REFERENCES person(id),
    sekvensnummer            SERIAL,
    payload                  JSON NOT NULL,
    type                     varchar(64),
    primary key(person_id, sekvensnummer)
);

