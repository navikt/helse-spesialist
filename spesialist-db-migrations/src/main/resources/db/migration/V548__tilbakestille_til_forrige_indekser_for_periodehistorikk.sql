drop index periodehistorikk_generasjon_id_utbetaling_id_idx;

create index on periodehistorikk (utbetaling_id);
create index on periodehistorikk (generasjon_id);
