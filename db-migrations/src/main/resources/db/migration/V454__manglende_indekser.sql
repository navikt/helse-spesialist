-- Indekser som mangler under normal drift, i følge query insights i GCP console

create index if not exists oppgave_command_context_id_idx
    on oppgave (command_context_id);
