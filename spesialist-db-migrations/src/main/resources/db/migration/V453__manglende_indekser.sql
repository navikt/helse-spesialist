-- Indekser som mangler under normal drift, i følge query insights i GCP console

create index if not exists avviksvurdering_fødselsnummer_idx
    on avviksvurdering (fødselsnummer);

create index if not exists automatisering_vedtaksperiode_ref
    on automatisering (vedtaksperiode_ref);
