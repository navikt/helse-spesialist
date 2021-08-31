ALTER TABLE notat
    DROP COLUMN saksbehandler_navn;
ALTER TABLE notat
    ADD CONSTRAINT notat_saksbehandler_ref_fkey FOREIGN KEY (saksbehandler_oid) REFERENCES saksbehandler (oid);
