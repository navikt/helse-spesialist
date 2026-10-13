DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-migrering')
    THEN
        REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM "spesialist-migrering";
        REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM "spesialist-migrering";
        REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM "spesialist-migrering";
        DROP USER "spesialist-migrering";
    END IF;
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-sidegig')
    THEN
        REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM "spesialist-sidegig";
        REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM "spesialist-sidegig";
        REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM "spesialist-sidegig";
        DROP USER "spesialist-sidegig";
    END IF;
END$$;