DO $$BEGIN
IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-opprydding-dev')
THEN GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO "spesialist-opprydding-dev";
END IF;
END$$;
