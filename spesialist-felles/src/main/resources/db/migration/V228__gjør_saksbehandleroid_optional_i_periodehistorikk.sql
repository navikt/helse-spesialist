-- For å ta høyde for automatisk behandling fra Spleis sin side (vedtaksperiode reberegnet)

ALTER TABLE periodehistorikk ALTER COLUMN saksbehandler_oid DROP NOT NULL;