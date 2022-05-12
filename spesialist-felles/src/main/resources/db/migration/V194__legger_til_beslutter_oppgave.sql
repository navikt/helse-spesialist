ALTER TABLE oppgave
    ADD COLUMN er_beslutter_oppgave BOOLEAN DEFAULT false NOT NULL;
ALTER TABLE oppgave
    ADD COLUMN er_retur_oppgave BOOLEAN DEFAULT false NOT NULL;

-- mellomlagring for å kunne tildele sak til opprinnelig saksbehandler hvis sak blir returnert fra beslutter
ALTER TABLE oppgave
    ADD COLUMN tidligere_saksbehandler_oid UUID;
