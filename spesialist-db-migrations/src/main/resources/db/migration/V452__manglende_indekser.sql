-- Indekser som mangler under normal drift, i følge query insights i GCP console

create index if not exists automatisering_utbetaling_id_idx
    on automatisering (utbetaling_id);

create index if not exists arbeidsforhold_arbeidsgiver_ref_person_ref_idx
    on arbeidsforhold (arbeidsgiver_ref, person_ref)
