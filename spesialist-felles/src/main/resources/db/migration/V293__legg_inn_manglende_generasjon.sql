with gen_id as (
    insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
        values (gen_random_uuid(), '68707645-8996-4d6f-a56b-011934e211a5', 'c7de7b7b-c4d1-4b9a-8b4c-e30b0023fbda', '2022-12-20T12:54:21.289313132', '15e9603b-48c9-490e-b9fd-6e975928c0db')
        returning id
)

insert into selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
values (gen_random_uuid(), 'RV_IM_2', '68707645-8996-4d6f-a56b-011934e211a5', '2022-12-20T12:54:21.313515407', (select * from gen_id)),
       (gen_random_uuid(), 'RV_IV_1', '68707645-8996-4d6f-a56b-011934e211a5', '2022-12-20T12:54:25.891360071', (select * from gen_id));

UPDATE oppgave SET status = 'AvventerSaksbehandler'::oppgavestatus, ferdigstilt_av = null, ferdigstilt_av_oid = null WHERE id = 2889361;
