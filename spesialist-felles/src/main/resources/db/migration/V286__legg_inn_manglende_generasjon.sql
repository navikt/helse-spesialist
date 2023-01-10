INSERT INTO selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse, låst_tidspunkt, låst_av_hendelse, låst)
VALUES (
        '29880556-fe3c-4d2a-998e-0aa488a3a551',
        '66fef445-6a98-4c3e-9f01-a8a473577482',
        'de684fa8-e621-4c32-9aaf-20470f3bc04b',
        '2022-12-19 10:54:19.875619516'::timestamp,
        '03f62bba-6005-499a-bc37-0f2f9d469237',
        null,
        null,
        false
);

INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
VALUES ('a820b640-f7ed-445f-b924-76c5e4a7ced8', 'RV_IM_1', '66fef445-6a98-4c3e-9f01-a8a473577482', '2022-12-19 10:54:19.872'::timestamp, (SELECT id FROM selve_vedtaksperiode_generasjon svg WHERE svg.vedtaksperiode_id = '66fef445-6a98-4c3e-9f01-a8a473577482' AND svg.utbetaling_id = 'de684fa8-e621-4c32-9aaf-20470f3bc04b' LIMIT 1)),
       ('4df01014-3892-4f3d-ad40-971946f1d14a', 'RV_IM_2', '66fef445-6a98-4c3e-9f01-a8a473577482', '2022-12-19 10:54:19.873'::timestamp, (SELECT id FROM selve_vedtaksperiode_generasjon svg WHERE svg.vedtaksperiode_id = '66fef445-6a98-4c3e-9f01-a8a473577482' AND svg.utbetaling_id = 'de684fa8-e621-4c32-9aaf-20470f3bc04b' LIMIT 1)),
       ('7e0e09e6-dcec-4e4c-aa79-c032720202bc', 'RV_RV_1', '66fef445-6a98-4c3e-9f01-a8a473577482', '2022-12-19 10:54:19.872'::timestamp, (SELECT id FROM selve_vedtaksperiode_generasjon svg WHERE svg.vedtaksperiode_id = '66fef445-6a98-4c3e-9f01-a8a473577482' AND svg.utbetaling_id = 'de684fa8-e621-4c32-9aaf-20470f3bc04b' LIMIT 1))
;

UPDATE oppgave SET status = 'AvventerSaksbehandler'::oppgavestatus, ferdigstilt_av = null, ferdigstilt_av_oid = null WHERE id = 2885824;