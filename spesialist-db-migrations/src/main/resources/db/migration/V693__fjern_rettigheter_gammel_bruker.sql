DO $$BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'eirik_gallefoss_temp') THEN
        REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM "eirik_gallefoss_temp";
        REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM "eirik_gallefoss_temp";

        ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public
            REVOKE ALL PRIVILEGES ON TABLES FROM "eirik_gallefoss_temp";
        ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public
            REVOKE ALL PRIVILEGES ON SEQUENCES FROM "eirik_gallefoss_temp";
    END IF;
END $$;