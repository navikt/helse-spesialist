DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-migrering')
    THEN
        REASSIGN OWNED BY "spesialist-migrering" TO CURRENT_USER;
        DROP OWNED BY "spesialist-migrering";
        DROP USER "spesialist-migrering";
    END IF;
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-sidegig')
    THEN
        REASSIGN OWNED BY "spesialist-sidegig" TO CURRENT_USER;
        DROP OWNED BY "spesialist-sidegig";
        DROP USER "spesialist-sidegig";
    END IF;
END$$;
