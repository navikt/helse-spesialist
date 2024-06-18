ALTER TABLE overstyring ADD COLUMN vedtaksperiode_id UUID;

CREATE CONSTRAINT TRIGGER sjekk_vedtaksperiode_id_not_null
    AFTER INSERT ON overstyring
    FOR EACH ROW
    WHEN (NEW.vedtaksperiode_id IS NULL)
EXECUTE FUNCTION kast_exception('VedtaksperiodeId kan ikke være null');


