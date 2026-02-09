create unique index ny_index on saksbehandleroppgavetype (vedtak_ref)
include (type, inntektskilde);

alter table saksbehandleroppgavetype drop constraint saksbehandleroppgavetype_vedtak_ref_key;
drop index if exists saksbehandleroppgavetype_vedtak_ref_key;

alter index ny_index rename to saksbehandleroppgavetype_vedtak_ref_key;
alter table saksbehandleroppgavetype add constraint saksbehandleroppgavetype_vedtak_ref_key unique using index saksbehandleroppgavetype_vedtak_ref_key;
