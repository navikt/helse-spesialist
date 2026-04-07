DROP INDEX idx_automatisering_opprettet_auto_true;

CREATE INDEX idx_automatisering_opprettet_auto_true
    ON automatisering(opprettet, vedtaksperiode_ref)
    WHERE automatisert = true;
