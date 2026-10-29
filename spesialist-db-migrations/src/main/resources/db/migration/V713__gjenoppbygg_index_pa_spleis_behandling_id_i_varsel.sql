DROP INDEX CONCURRENTLY IF EXISTS varsel_spleis_behandling_id_idx;
CREATE INDEX CONCURRENTLY varsel_spleis_behandling_id_idx ON varsel (spleis_behandling_id);
