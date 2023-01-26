/*

Finne åpne oppgaver hvor nyeste generasjon er låst:

select v.vedtaksperiode_id from oppgave o
join vedtak v on o.vedtak_ref = v.id
join saksbehandleroppgavetype s on v.id = s.vedtak_ref
where o.status = 'AvventerSaksbehandler' and
        (select låst from selve_vedtaksperiode_generasjon svg
         where svg.vedtaksperiode_id = v.vedtaksperiode_id order by svg.opprettet_tidspunkt desc limit 1) = true
  and o.type = 'SØKNAD';

Finne data til generasjonen:

select data->>'vedtaksperiodeId' as vedtaksperiode_id, data->>'utbetalingId' as utbetaling_id, data->>'@opprettet' as opprettet_tidspunkt, hendelse.id as opprettet_av_id from hendelse
JOIN person p on hendelse.fodselsnummer = p.fodselsnummer
JOIN vedtak v on p.id = v.person_ref
where type = 'VEDTAKSPERIODE_NY_UTBETALING' AND v.vedtaksperiode_id in (<vedtaksperiode_id>);

Finne varselkoder (ikke nødvendigvis komplett liste, det er ikke alle varselkoder som ligger i spesialist-basen (per nå)):
select json_array_elements(data->'aktiviteter')->>'varselkode' as varselkode, data->>'@opprettet' as opprettet from hendelse
JOIN person p on hendelse.fodselsnummer = p.fodselsnummer
JOIN vedtak v on p.id = v.person_ref
where type = 'NYE_VARSLER' AND v.vedtaksperiode_id in (<vedtaksperiode_id>);

*/

insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
        values (gen_random_uuid(), '3bc28bf0-2568-4e32-9701-b55b8c0e81e6', 'e074cf70-cc51-4c33-b2da-2ed87365a0c5', '2022-12-19T12:07:20.293595804', '37f00d95-953a-4314-8312-446b8713170c');

insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
    values (gen_random_uuid(), 'cfe6f5fc-c22d-432c-a22b-9bb23a1f825c', '1bf04242-6792-4601-8f73-223b81157adc', '2022-12-19T13:16:45.835821342', '4d164bab-ada2-41bb-a7d2-0fc3829b4b41');

insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
    values (gen_random_uuid(), '784b97ca-c7b6-4308-89a1-17dcff1c502f', '4845e8f2-eaa5-454b-a713-6740a39f0726', '2022-12-20T09:47:15.046671002', 'dea27778-1b0e-4c77-b2af-3dd0fd0af7b6');

insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
    values (gen_random_uuid(), '50b2a05e-787e-4e77-8abd-7f5767515617', '5c9dbeb7-7208-4675-8a33-a707201f803a', '2022-12-20T12:56:28.448355944', 'e4345a3f-ec11-404b-9cf2-85267f3cda87');

insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
values (gen_random_uuid(), 'ec552d39-851b-46ad-a3aa-ad800ee6b4b4', '22576f44-b2e7-4769-b99b-6949e7669d2f', '2022-12-21T08:38:02.429761845', '3ff4aada-3a35-4366-991b-9248528953de');

with gen_id as (
    insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
        values (gen_random_uuid(), '1935e0af-64dc-4f73-99e8-8370717b404a', '53fb07a2-67d2-4e1b-a9ed-ecfe1baeac09', '2022-12-21T12:58:57.886882866', 'bdac8d8a-9b6d-4a9f-a08b-8f6b8d29a3b8')
   returning id
)
insert into selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
values (gen_random_uuid(), 'RV_IT_15', '1935e0af-64dc-4f73-99e8-8370717b404a', '2022-12-21T12:58:57.92745375', (select * from gen_id));

with gen_id as (
    insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
        values (gen_random_uuid(), 'edc9b4a8-04f1-4f47-9a58-5faf27fe8d52', '7b95189f-107e-4764-ae76-6624b8c4a099', '2022-12-22T09:04:51.225325359', '44f242d5-4eb3-44c4-aca3-7d3fc7c5e1bd')
   returning id
)
insert into selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
values (gen_random_uuid(), 'RV_SØ_9', 'edc9b4a8-04f1-4f47-9a58-5faf27fe8d52', '2022-11-01T08:04:07.391604046', (select * from gen_id));

with gen_id as (
    insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
        values (gen_random_uuid(), '2ccfd41c-6209-4dd9-848f-5f0016b75503', '5b0fa4b7-12ae-4527-a26b-eb2a353a7ba7', '2022-12-22T09:59:46.778861509', '367b110c-5033-4307-84c5-bc5e34fe363f')
   returning id
)
insert into selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
values (gen_random_uuid(), 'RV_IM_2', '2ccfd41c-6209-4dd9-848f-5f0016b75503', '2022-07-12T15:12:17', (select * from gen_id));

insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
    values (gen_random_uuid(), '102f4f89-6068-4f92-b7ef-0374026d950e', '8236bc94-ed0b-4549-8f24-497215a033de', '2022-12-23T10:48:06.381950327', '3175bad1-7b5d-439d-a9c5-305784ae417e');

insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
    values (gen_random_uuid(), 'e0463b4c-42bf-4101-ba95-c6e78e4c8a3c', '825eff69-de63-4ce7-bb92-298f0b5278e8', '2022-12-23T14:12:01.90293961', '71946e6a-dd27-466b-8fa7-d0cf206cdcb1');

with gen_id as (
    insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
        values (gen_random_uuid(), '2b1a2278-2488-460c-bc5e-8323e23675e0', '3d65b3d8-a6be-4b40-9483-bb17617de5f7', '2022-12-27T16:36:35.036660868', 'b792b5ec-f7c6-4c7e-ab72-927f9f3b0977')
   returning id
)
insert into selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
values (gen_random_uuid(), 'RV_IM_4', '2b1a2278-2488-460c-bc5e-8323e23675e0', '2022-12-27T16:36:31.522576376', (select * from gen_id));

with gen_id as (
    insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
        values (gen_random_uuid(), '506fadbc-fe3b-4ebb-b594-08433dd489aa', 'eba6c1f9-8c35-45b5-a715-5478e0aaaa5e', '2022-12-28T17:47:37.956862375', '211cf30f-dc5b-4f03-b852-3e7b67c677fb')
   returning id
)
insert into selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
values (gen_random_uuid(), 'RV_IV_1', '506fadbc-fe3b-4ebb-b594-08433dd489aa', '2022-12-28T17:47:37.69651972', (select * from gen_id));

with gen_id as (
    insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
        values (gen_random_uuid(), '79d854d0-0cfa-414e-a031-ea646504b36b', 'fd5582d9-aaf4-47a1-b4e1-c193bbe6ca8b', '2022-12-28T18:55:29.656438057', 'f9cee6a6-ae43-448c-94c0-f4fa5609d8d0')
   returning id
)
insert into selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
values (gen_random_uuid(), 'RV_IV_1', '79d854d0-0cfa-414e-a031-ea646504b36b', '2022-12-28T18:55:29.452562318', (select * from gen_id));

insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
    values (gen_random_uuid(), '52c26ac4-8ce9-402b-88d0-a0e95559ab47', '37196b5a-12e7-4811-98bd-70ca52b0fcba', '2022-12-29T14:00:42.900283181', 'b75a8abd-03f3-4ecb-b616-d5c86cf66e9f');

with gen_id as (
    insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
        values (gen_random_uuid(), 'ed1357db-9dc1-4430-9794-acbf2aab68f9', '6f49007f-df1c-4319-aecf-0e6bceb92ca5', '2023-01-03T19:56:02.177856276', '68e8e844-e87f-4f8d-bc12-f986b0a40456')
   returning id
)
insert into selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
values (gen_random_uuid(), 'RV_IM_3', 'ed1357db-9dc1-4430-9794-acbf2aab68f9', '2023-01-03T19:55:53.80620444', (select * from gen_id));

with gen_id as (
    insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
        values (gen_random_uuid(), '885dc281-2e0a-4949-ae5b-7a07f87b40a9', '48ed7d4f-3dea-413f-9f6c-d96e8fe200c7', '2023-01-04T14:07:10.686980096', '78f59361-af6a-486b-8660-3872d4801d66')
   returning id
)
insert into selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
values (gen_random_uuid(), 'RV_IV_1', '885dc281-2e0a-4949-ae5b-7a07f87b40a9', '2023-01-04T14:07:10.338090943', (select * from gen_id));

insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
    values (gen_random_uuid(), '07e1edb8-3e78-4914-8dad-d9572c786b8a', '0b2de9d6-dba8-4039-b867-c7b0c551db0a', '2023-01-04T15:23:26.867893816', '47ebe2f4-8e9e-492b-9636-bf17113129b5');

insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
    values (gen_random_uuid(), '9a5116b7-6375-498d-9da3-8dd1eccc3673', 'f5090a27-311a-4df3-b0b8-620f44c6138a', '2023-01-05T13:06:27.586090732', '45a32a42-b313-4616-876c-334d1613c19a');

insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
    values (gen_random_uuid(), '0cd691e2-c489-49b2-8853-4accf85b1e46', '834689fa-db41-4903-a80a-79b116313a0b', '2023-01-06T11:25:16.159419479', 'faed3b2b-4f7a-4938-99f5-fc0c8d88dbde');

with gen_id as (
    insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
        values (gen_random_uuid(), '83a73be3-eeef-4677-b6c7-0574fce50142', '41c8a0b7-d13f-4039-b20f-01652127f25f', '2023-01-09T10:08:51.683799443', 'e6cb3ab8-10f3-493e-bcef-b257c9e5542d')
   returning id
)
insert into selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
values (gen_random_uuid(), 'RV_IV_3', '83a73be3-eeef-4677-b6c7-0574fce50142', '2023-01-09T10:08:51.449568868', (select * from gen_id)),
       (gen_random_uuid(), 'RV_IV_1', '83a73be3-eeef-4677-b6c7-0574fce50142', '2023-01-09T10:08:51.449568868', (select * from gen_id));

with gen_id as (
    insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
        values (gen_random_uuid(), 'aa7e4fe6-df86-4976-bd22-38c200485f08', '78a07175-57d2-4b6e-af4a-8eb6cf2d8be2', '2023-01-12T14:16:51.74857236', '93dc72f3-b009-48d5-97dd-85ed77b6389d')
    returning id
)
insert into selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
values (gen_random_uuid(), 'RV_IV_3', 'aa7e4fe6-df86-4976-bd22-38c200485f08', '2023-01-12T14:16:51.482329713', (select * from gen_id));

with gen_id as (
    insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
        values (gen_random_uuid(), '058436ed-0b3d-45fc-b678-e27931e0fda0', '961f0fd4-4b23-4ac2-a648-a40fb4ca8a63', '2022-12-15T14:21:26.215744572', '08b4633c-f699-4358-8938-fbb69ab5d7a3')
    returning id
)
insert into selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
values (gen_random_uuid(), 'RV_MV_1', '058436ed-0b3d-45fc-b678-e27931e0fda0', '2022-12-15T14:21:24.750514633', (select * from gen_id));

insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse)
    values (gen_random_uuid(), 'aa3bc6b7-aba6-4e1d-a365-f4be05bed63a', '9cb7530a-f88d-4965-b8b1-2659372ef665', '2023-01-13T14:33:43.455768741', 'a26b4364-21c5-4869-9550-003584ad6dbd');


