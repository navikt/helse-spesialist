UPDATE oppgave o SET behandling_id = svg.spleis_behandling_id
    FROM selve_vedtaksperiode_generasjon svg WHERE svg.unik_id = o.generasjon_ref
AND o.behandling_id IS NULL;
