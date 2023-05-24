UPDATE selve_varsel
SET status = 'AKTIV', definisjon_ref = null
WHERE unik_id = '7cc4689c-9aaa-4e3e-a5b4-ea9d0e5d0f16';

UPDATE selve_vedtaksperiode_generasjon
SET tilstand = 'UtenUtbetalingMåVurderes'
WHERE unik_id = '72437040-c265-43cb-9b2b-7bdaddecbee9';