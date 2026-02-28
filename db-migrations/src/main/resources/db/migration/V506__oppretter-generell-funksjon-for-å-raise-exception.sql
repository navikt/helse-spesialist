CREATE OR REPLACE FUNCTION kast_exception()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    msg text := TG_ARGV[0]::text;
BEGIN
    RAISE EXCEPTION '%', msg;
END;$$;


