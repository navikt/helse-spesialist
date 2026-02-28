DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'cloudsqliamuser')
        THEN
            grant delete, insert, references, select, trigger, truncate, update on vedtak to cloudsqliamuser;
        END IF;
    END
$$;