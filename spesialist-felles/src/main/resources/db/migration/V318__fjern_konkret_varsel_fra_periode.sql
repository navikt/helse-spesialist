UPDATE selve_varsel SET status = 'INAKTIV', status_endret_ident = 'Manuelt behandlet', status_endret_tidspunkt = now()
WHERE kode IN ('RV_UT_1', 'SB_EX_1') AND generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon svg WHERE svg.unik_id = '859e84df-ad72-4754-947a-7fbc997a2274');