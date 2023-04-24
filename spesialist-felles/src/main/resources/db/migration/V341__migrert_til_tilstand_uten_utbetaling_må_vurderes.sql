UPDATE selve_vedtaksperiode_generasjon svg
    SET tilstand = 'UtenUtbetalingMåVurderes'
    FROM selve_varsel sv
    WHERE sv.generasjon_ref = svg.id AND tilstand = 'AvsluttetUtenUtbetaling' AND svg.utbetaling_id IS NULL AND sv.status = 'AKTIV';