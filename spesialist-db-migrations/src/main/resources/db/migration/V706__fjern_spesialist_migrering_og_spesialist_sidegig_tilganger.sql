DO $$
DECLARE
    rolle text;
    statement text;
BEGIN
    FOREACH rolle IN ARRAY ARRAY['spesialist-migrering', 'spesialist-sidegig']
    LOOP
        CONTINUE WHEN NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = rolle);

        FOREACH statement IN ARRAY ARRAY[
            format('REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM %I', rolle),
            format('REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM %I', rolle),
            format('REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM %I', rolle),
            format('REVOKE ALL PRIVILEGES ON TABLE flyway_schema_history FROM %I', rolle),
            format('REVOKE ALL PRIVILEGES ON SCHEMA public FROM %I', rolle),
            format('REVOKE ALL PRIVILEGES ON DATABASE %I FROM %I', current_database(), rolle),
            format('ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public REVOKE ALL PRIVILEGES ON TABLES FROM %I', rolle),
            format('ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public REVOKE ALL PRIVILEGES ON SEQUENCES FROM %I', rolle)
        ]
        LOOP
            BEGIN
                EXECUTE statement;
            EXCEPTION WHEN insufficient_privilege OR undefined_object OR undefined_table THEN
                RAISE NOTICE 'Hoppet over "%": %', statement, SQLERRM;
            END;
        END LOOP;
    END LOOP;
END$$;
