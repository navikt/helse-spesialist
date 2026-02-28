ALTER TABLE oppgave ADD COLUMN generasjon_ref UUID;

UPDATE oppgave o SET generasjon_ref = (
    SELECT unik_id FROM selve_vedtaksperiode_generasjon svg WHERE vedtaksperiode_id = (
        SELECT vedtaksperiode_id FROM vedtak v WHERE v.id = o.vedtak_ref
    ) ORDER BY id DESC LIMIT 1
) WHERE status = 'AvventerSaksbehandler';