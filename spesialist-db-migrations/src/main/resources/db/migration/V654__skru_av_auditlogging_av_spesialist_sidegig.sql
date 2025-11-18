DO $$ BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-sidegig')
    THEN
        ALTER USER "spesialist-sidegig" IN DATABASE "spesialist" SET pgaudit.log TO 'none';
    END IF;
END $$;
