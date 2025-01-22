drop index periodehistorikk_utbetaling_id_idx;

drop index periodehistorikk_generasjon_id_idx;

-- Håper denne får en slutt på Seq Scan, ut fra en teori om at query planneren ikke bruker
-- de indexene på enkeltfeltene pga. en OR i spørringen.
create index on periodehistorikk (generasjon_id, utbetaling_id);
