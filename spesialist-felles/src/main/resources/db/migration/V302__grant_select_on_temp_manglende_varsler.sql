DO
$do$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='spesialist') THEN
            GRANT SELECT ON temp_manglende_varsler TO "spesialist";
        END IF;
    END
$do$