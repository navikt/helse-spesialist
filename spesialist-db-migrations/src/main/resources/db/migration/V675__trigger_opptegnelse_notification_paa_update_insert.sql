CREATE OR REPLACE FUNCTION notify_opptegnelse()
    RETURNS trigger AS
$$
BEGIN
    PERFORM pg_notify('opptegnelse', json_build_object(
            'personId', NEW.person_id
    )::text);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER opptegnelse_notify
    AFTER INSERT OR UPDATE
    ON opptegnelse
    FOR EACH ROW
EXECUTE FUNCTION notify_opptegnelse();
