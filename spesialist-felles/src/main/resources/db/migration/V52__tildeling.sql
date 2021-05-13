CREATE TABLE tildeling(saksbehandler_ref UUID NOT NULL REFERENCES saksbehandler(oid), oppgave_ref UUID NOT NULL);
