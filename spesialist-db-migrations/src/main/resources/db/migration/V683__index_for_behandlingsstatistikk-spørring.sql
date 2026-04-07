CREATE INDEX idx_automatisering_opprettet_auto_true
    ON automatisering(opprettet)
    WHERE automatisert = true;
