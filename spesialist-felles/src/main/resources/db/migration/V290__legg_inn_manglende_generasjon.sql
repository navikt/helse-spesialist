INSERT INTO selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse, låst_tidspunkt, låst_av_hendelse, låst)
VALUES (
        'c0caaaee-3537-438d-a358-ddb70ea81fd0',
        '4966949a-afcd-4cbb-8de4-dd328461532d',
        '9bfbae5c-0e9d-4a7a-85b8-e46cdac9a2df',
        '2022-12-15 09:32:52.759753507'::timestamp,
        '0e66525a-67e4-4a3e-93ee-86b880edff99',
        null,
        null,
        false
);

INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
VALUES ('848ff962-8334-4d53-a477-49a709c52175', 'RV_RV_1', '4966949a-afcd-4cbb-8de4-dd328461532d', '2022-12-15 09:27:01'::timestamp, (SELECT id FROM selve_vedtaksperiode_generasjon svg WHERE svg.vedtaksperiode_id = '4966949a-afcd-4cbb-8de4-dd328461532d' AND svg.utbetaling_id = '9bfbae5c-0e9d-4a7a-85b8-e46cdac9a2df' LIMIT 1))
;

UPDATE oppgave SET status = 'AvventerSaksbehandler'::oppgavestatus, ferdigstilt_av = null, ferdigstilt_av_oid = null WHERE id = 2883456;
