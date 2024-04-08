CREATE type generasjon_tilstand AS ENUM ('Åpen', 'Låst', 'AvsluttetUtenUtbetaling', 'UtenUtbetalingMåVurderes');

ALTER TABLE selve_vedtaksperiode_generasjon ALTER COLUMN tilstand TYPE generasjon_tilstand USING tilstand::generasjon_tilstand;