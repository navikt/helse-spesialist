DO
$do$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='spesialist-opprydding-dev') THEN
            GRANT SELECT, DELETE ON ALL TABLES IN SCHEMA public TO "spesialist-opprydding-dev";
        END IF;
    END
$do$
