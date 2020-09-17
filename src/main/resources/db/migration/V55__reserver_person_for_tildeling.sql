ALTER TABLE tildeling ADD COLUMN gyldig_til TIMESTAMP DEFAULT NULL;

CREATE TABLE reserver_person(
    saksbehandler_ref UUID NOT NULL REFERENCES saksbehandler(oid),
    person_ref BIGINT NOT NULL REFERENCES person(id),
    gyldig_til TIMESTAMP NOT NULL DEFAULT now() + INTERVAL '12 HOURS');

ALTER TABLE command_context ALTER COLUMN vedtaksperiode_id DROP NOT NULL;
