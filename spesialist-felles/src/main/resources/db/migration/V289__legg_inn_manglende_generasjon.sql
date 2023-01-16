INSERT INTO selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse, låst_tidspunkt, låst_av_hendelse, låst)
VALUES (
        '9745b633-dccb-4040-af0f-8c20d43bb43c',
        'c0f95cd7-b34d-4e0b-b9b1-a87de1ef0eef',
        '42c6d04f-bd7a-465e-a28a-6b7ba43f2719',
        '2023-01-13 14:24:08.950179368'::timestamp,
        '039120cf-2dcd-49df-b11a-229a3b767b7e',
        null,
        null,
        false
);

INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
VALUES ('5daf6ee3-7182-416c-9e5c-647db2308bd8', 'RV_IV_2', 'c0f95cd7-b34d-4e0b-b9b1-a87de1ef0eef', '2023-01-13 14:24:10.969'::timestamp, (SELECT id FROM selve_vedtaksperiode_generasjon svg WHERE svg.vedtaksperiode_id = 'c0f95cd7-b34d-4e0b-b9b1-a87de1ef0eef' AND svg.utbetaling_id = '42c6d04f-bd7a-465e-a28a-6b7ba43f2719' LIMIT 1)),
       ('d910f590-5bef-4342-826c-cb91c633dfa9', 'RV_RV_1', 'c0f95cd7-b34d-4e0b-b9b1-a87de1ef0eef', '2023-01-13 14:24:08.949'::timestamp, (SELECT id FROM selve_vedtaksperiode_generasjon svg WHERE svg.vedtaksperiode_id = 'c0f95cd7-b34d-4e0b-b9b1-a87de1ef0eef' AND svg.utbetaling_id = '42c6d04f-bd7a-465e-a28a-6b7ba43f2719' LIMIT 1))
;

UPDATE oppgave SET status = 'AvventerSaksbehandler'::oppgavestatus, ferdigstilt_av = null, ferdigstilt_av_oid = null WHERE id = 2911023;