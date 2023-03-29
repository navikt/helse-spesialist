UPDATE selve_vedtaksperiode_generasjon svg SET fom = v.fom, tom = v.tom
FROM vedtak v WHERE svg.vedtaksperiode_id = v.vedtaksperiode_id AND svg.fom IS NULL;