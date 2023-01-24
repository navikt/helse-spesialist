INSERT INTO selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse, låst_tidspunkt, låst_av_hendelse, låst)
VALUES (
        '59abb58e-4f7c-412a-8fee-d5ae673322e8',
        'df7a2486-7862-4119-b85c-cd74e1f81270',
        'da316c04-13a3-4a8b-9f78-8d54e2d9e246',
        '2022-12-15 17:21:23.101753685'::timestamp,
        '0cc41b4e-e7cd-4497-95d2-a6943a26e259',
        null,
        null,
        false
);

INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
VALUES ('9b8eb081-d5c8-4a66-ac8b-9275d13a0132', 'RV_RV_1', 'df7a2486-7862-4119-b85c-cd74e1f81270', '"2022-12-15 17:15:34"'::timestamp, (SELECT id FROM selve_vedtaksperiode_generasjon svg WHERE svg.vedtaksperiode_id = 'df7a2486-7862-4119-b85c-cd74e1f81270' AND svg.utbetaling_id = 'da316c04-13a3-4a8b-9f78-8d54e2d9e246' LIMIT 1))
;

UPDATE oppgave SET status = 'AvventerSaksbehandler'::oppgavestatus, ferdigstilt_av = null, ferdigstilt_av_oid = null WHERE id = 2884226;
