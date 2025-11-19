DO $$ BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-migrations')
    THEN
        ALTER USER "spesialist-migrations" IN DATABASE "spesialist" SET pgaudit.log TO 'none';
    END IF;
END $$;
