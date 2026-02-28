drop index if exists tildeling_oppgave_id_ref_idx;
drop index if exists tildeling_oppgave_id_ref_saksbehandler_ref_idx;

create index if not exists tildeling_saksbehandler_ref_oppgave_id_ref_idx
    on tildeling (saksbehandler_ref)
    include (oppgave_id_ref);
