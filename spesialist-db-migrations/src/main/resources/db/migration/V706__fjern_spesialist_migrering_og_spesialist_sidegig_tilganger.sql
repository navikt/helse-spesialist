DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-migrering')
    THEN
        GRANT "spesialist-migrering" TO "spesialist-db-migrations";
        REASSIGN OWNED BY "spesialist-migrering" TO "spesialist-db-migrations";
        DROP OWNED BY "spesialist-migrering";
        DROP USER "spesialist-migrering";
    END IF;
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-sidegig')
    THEN
        GRANT "spesialist-sidegig" TO "spesialist-db-migrations";
        REASSIGN OWNED BY "spesialist-sidegig" TO "spesialist-db-migrations";
        DROP OWNED BY "spesialist-sidegig";
        DROP USER "spesialist-sidegig";
    END IF;
END$$;
