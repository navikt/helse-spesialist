DO
$do$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='spesialist') THEN
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO "spesialist";
        END IF;
    END
$do$