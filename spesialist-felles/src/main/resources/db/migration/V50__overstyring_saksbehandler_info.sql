CREATE TABLE saksbehandler(oid UUID NOT NULL PRIMARY KEY, navn VARCHAR(64), epost VARCHAR(128));

DELETE FROM overstyrtdag;
DELETE FROM overstyring;
ALTER TABLE overstyring
    ADD COLUMN saksbehandler_ref UUID NOT NULL REFERENCES saksbehandler(oid);

