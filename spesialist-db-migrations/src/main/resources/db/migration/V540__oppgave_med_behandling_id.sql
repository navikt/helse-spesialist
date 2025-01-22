ALTER TABLE oppgave ADD COLUMN behandling_id UUID;
CREATE INDEX ON oppgave(behandling_id);

UPDATE oppgave o SET behandling_id = svg.spleis_behandling_id
    FROM selve_vedtaksperiode_generasjon svg WHERE svg.unik_id = o.generasjon_ref
AND o.status IN ('AvventerSaksbehandler'::oppgavestatus, 'AvventerSystem'::oppgavestatus);
