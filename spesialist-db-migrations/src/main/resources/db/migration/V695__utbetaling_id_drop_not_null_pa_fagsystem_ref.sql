-- Release 1 av 2: gjør fagsystem-ref-kolonnene nullable slik at ny kode kan slutte å skrive
-- til dem mens gamle podder fortsatt skriver. Kolonnene og oppdrag-tabellen droppes i V696,
-- som først kan deployes etter at denne releasen er ute i prod.
--
-- Bakgrunn: oppdrag-tabellen har aldri blitt lest av noe. Eneste SQL mot den i produksjonskode
-- var en INSERT i PgUtbetalingDao.nyttOppdrag.
ALTER TABLE utbetaling_id
    ALTER COLUMN arbeidsgiver_fagsystem_id_ref DROP NOT NULL,
    ALTER COLUMN person_fagsystem_id_ref DROP NOT NULL;
