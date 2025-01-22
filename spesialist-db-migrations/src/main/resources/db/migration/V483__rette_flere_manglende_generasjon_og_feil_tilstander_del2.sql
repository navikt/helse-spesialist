update selve_vedtaksperiode_generasjon set tilstand='AvsluttetUtenVedtak', spleis_behandling_id='7277dc99-c883-4fde-b7e8-40b3d7fdc62b' where id=11326376;
update selve_vedtaksperiode_generasjon set tilstand='AvsluttetUtenVedtak' where id=23419617;
update selve_vedtaksperiode_generasjon set tilstand='AvsluttetUtenVedtak' where id=23519437;
update selve_vedtaksperiode_generasjon set tilstand='AvsluttetUtenVedtak', spleis_behandling_id='c548d637-7300-49df-837a-bd5e916f1749' where id=10961334;
update selve_vedtaksperiode_generasjon set spleis_behandling_id='deac596c-5386-4d79-a706-30fc5e71dbb6' where id=8028139;
update selve_vedtaksperiode_generasjon set tilstand='AvsluttetUtenVedtak' where id=8118507;
update selve_vedtaksperiode_generasjon set tilstand='AvsluttetUtenVedtak' where id=14929843;
update selve_vedtaksperiode_generasjon set tilstand='AvsluttetUtenVedtak', spleis_behandling_id='a6e61564-29fa-4af4-8385-b0423ef6c6a0' where id=10773150;
update selve_vedtaksperiode_generasjon set tilstand='AvsluttetUtenVedtak', spleis_behandling_id='5857b483-2d5a-4668-8f52-9f57457575a3' where id=10841188;
update selve_vedtaksperiode_generasjon set tilstand='AvsluttetUtenVedtak', spleis_behandling_id='57a6038f-8b71-41db-9303-65f4da431796' where id=11018625;
update selve_vedtaksperiode_generasjon set tilstand='AvsluttetUtenVedtak', spleis_behandling_id='dda38bc5-349f-41c3-89af-852ef5f56198' where id=11118498;
update selve_vedtaksperiode_generasjon set tilstand='AvsluttetUtenVedtak' where id=13905993;
update selve_vedtaksperiode_generasjon set tilstand='AvsluttetUtenVedtak', spleis_behandling_id='91ba1f51-3662-46c4-b37a-3859d97e1087' where id=17167567;
update selve_vedtaksperiode_generasjon set spleis_behandling_id='acf50a6b-5e4c-4a31-a59c-3e91eebc5148' where id=7873489;
update selve_vedtaksperiode_generasjon set tilstand='AvsluttetUtenVedtak', spleis_behandling_id='835ccc5c-62c7-42d3-84a5-831f9d225cee' where id=8816275;

update selve_vedtaksperiode_generasjon set tilstand = 'AvsluttetUtenVedtakMedVarsler', spleis_behandling_id='d56944d4-b7bf-4dc0-9987-41cdd8425e1d' where id=11827070;
insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, opprettet_av_hendelse,
                                             tilstand_endret_tidspunkt, fom, tom, skjæringstidspunkt, tilstand,
                                             spleis_behandling_id, tags)
values ('25ee14d4-1f47-46e3-83ff-ae6cca3d05f9',
        'e5ad3110-3a23-48f2-9433-62a56b3c860d',
        'deba8003-6d2f-41e7-8561-87676a3ce7f8',
        now(),
        '2024-02-19',
        '2024-02-25',
        '2024-02-12',
        'AvsluttetUtenVedtakMedVarsler',
        'f969d5d9-aa92-4fe6-82f1-9a7d6426d544',
        array []::char[]);
insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, opprettet_av_hendelse,
                                             tilstand_endret_tidspunkt, fom, tom, skjæringstidspunkt, tilstand,
                                             spleis_behandling_id, tags)
values ('dc6ef5c1-16ba-499d-b40a-02c5d08e516a',
        'e5ad3110-3a23-48f2-9433-62a56b3c860d',
        'f79c13af-dae8-4fa7-b8c6-f160599b3736',
        now(),
        '2024-02-19',
        '2024-02-25',
        '2024-02-12',
        'AvsluttetUtenVedtakMedVarsler',
        'fa33fb20-e626-46b0-977c-e24c93b00722',
        array []::char[]);
update selve_varsel set generasjon_ref=(select id from selve_vedtaksperiode_generasjon where unik_id='dc6ef5c1-16ba-499d-b40a-02c5d08e516a')
where vedtaksperiode_id='e5ad3110-3a23-48f2-9433-62a56b3c860d';

insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, opprettet_av_hendelse,
                                             tilstand_endret_tidspunkt, fom, tom, skjæringstidspunkt, tilstand,
                                             spleis_behandling_id, tags)
values ('5198a18f-896b-4095-8ab4-a7f3fe70fb1c',
        'e9ea51eb-aab9-4f8f-b37a-1a06ed4f6a58',
        '4864f265-6e82-4d13-9b32-91c77043194f',
        now(),
        '2024-01-13',
        '2024-02-11',
        '2024-02-01',
        'AvsluttetUtenVedtak',
        '7dc6884c-dcc4-44dd-a529-da6003cac6c7',
        array []::char[]);

update selve_vedtaksperiode_generasjon set spleis_behandling_id='992991b3-cc73-4811-8455-cd8a14bba720' where id=7949668;
update selve_vedtaksperiode_generasjon set tilstand = 'AvsluttetUtenVedtak', spleis_behandling_id='7d0b28cc-b912-4981-8000-e3365861bd19' where id=8174775;
insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, opprettet_av_hendelse,
                                             tilstand_endret_tidspunkt, fom, tom, skjæringstidspunkt, tilstand,
                                             spleis_behandling_id, tags)
values ('a06b2d67-b268-4155-b625-9795c90eb0ac',
        'cca87a61-b234-4926-abe4-a538058bcf10',
        '574138ad-e0d3-4c29-91a9-39613b3e80c5',
        now(),
        '2023-07-05',
        '2023-07-22',
        '2023-07-11',
        'AvsluttetUtenVedtak',
        'e5d5806b-66b9-4a81-a268-cbdc5e215581',
        array []::char[]);

update selve_vedtaksperiode_generasjon set tilstand = 'AvsluttetUtenVedtak', spleis_behandling_id='99cca5a7-5cfd-4358-81bc-091aa4762770' where id=14341180;
insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, opprettet_av_hendelse,
                                             tilstand_endret_tidspunkt, fom, tom, skjæringstidspunkt, tilstand,
                                             spleis_behandling_id, tags)
values ('668a6bc3-a589-49bb-8d6d-4cff34c8e0e1',
        'b2842e17-6106-4ce0-8613-f67e65003bf7',
        'c8070559-6707-4eba-b5f3-5553300ed987',
        now(),
        '2024-01-31',
        '2024-03-02',
        '2024-02-15',
        'KlarTilBehandling',
        'bef85277-2321-4bfd-bea0-ede33eb69eff',
        array []::char[]);
update selve_varsel set generasjon_ref=(select id from selve_vedtaksperiode_generasjon where unik_id='668a6bc3-a589-49bb-8d6d-4cff34c8e0e1')
where vedtaksperiode_id='b2842e17-6106-4ce0-8613-f67e65003bf7';

update selve_vedtaksperiode_generasjon set tilstand = 'AvsluttetUtenVedtak', spleis_behandling_id='92ffb347-259b-43a8-a25b-bdb048c36dfb' where id=10918234;
insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, opprettet_av_hendelse,
                                             tilstand_endret_tidspunkt, fom, tom, skjæringstidspunkt, tilstand,
                                             spleis_behandling_id, tags)
values ('8c5c27b6-6f48-4518-b166-e0795959d3f2',
        'e7331af7-d2eb-4161-ab3c-786d00ea462c',
        'ea3db873-2bd9-4d68-a845-23775acb2d8e',
        now(),
        '2024-01-24',
        '2024-02-07',
        '2024-01-31',
        'AvsluttetUtenVedtakMedVarsler',
        'e0f8ae66-bce8-4aed-9582-bcdb24460957',
        array []::char[]);
update selve_varsel set generasjon_ref=(select id from selve_vedtaksperiode_generasjon where unik_id='8c5c27b6-6f48-4518-b166-e0795959d3f2')
where vedtaksperiode_id='e7331af7-d2eb-4161-ab3c-786d00ea462c';

update selve_vedtaksperiode_generasjon set tilstand = 'AvsluttetUtenVedtak', spleis_behandling_id='d252f024-7546-4c26-998b-50465785b3c2' where id=14502987;
insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, opprettet_av_hendelse,
                                             tilstand_endret_tidspunkt, fom, tom, skjæringstidspunkt, tilstand,
                                             spleis_behandling_id, tags)
values ('62fda81e-6c02-4ef9-847f-d9ef27474002',
        '9ccea0a7-d689-42e7-b5e3-db27dd3519e2',
        'c95d33e2-9ea1-4696-a039-65bacc5c57cf',
        now(),
        '2024-02-20',
        '2024-03-10',
        '2024-02-27',
        'AvsluttetUtenVedtak',
        'fc498732-dbc2-4c57-87b5-e77cf81deb5a',
        array []::char[]);

update selve_vedtaksperiode_generasjon set tilstand = 'AvsluttetUtenVedtak', spleis_behandling_id='5f3851c7-598b-4b13-b19b-0ef461b82901' where id=15356419;
insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, opprettet_av_hendelse,
                                             tilstand_endret_tidspunkt, fom, tom, skjæringstidspunkt, tilstand,
                                             spleis_behandling_id, tags)
values ('e1c8206d-5b0f-4467-ad83-4f1bcba1e31a',
        '205a47ae-4fad-4e27-abda-fa73eb61fe7b',
        '14e8a92e-d36e-4a76-9224-c187a8239603',
        now(),
        '2024-02-19',
        '2024-03-13',
        '2024-03-05',
        'AvsluttetUtenVedtak',
        '9d541fcd-7976-4c70-8841-97acfc08a34e',
        array []::char[]);
insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, opprettet_av_hendelse,
                                             tilstand_endret_tidspunkt, fom, tom, skjæringstidspunkt, tilstand,
                                             spleis_behandling_id, tags)
values ('91ec00ec-8ff2-4eb4-9485-5189bdc1e058',
        '205a47ae-4fad-4e27-abda-fa73eb61fe7b',
        '8ed3bd74-3892-427e-880b-c98eb2a263a0',
        now(),
        '2024-02-19',
        '2024-03-13',
        '2024-03-05',
        'AvsluttetUtenVedtak',
        'ee417f65-2ccb-4ddd-9814-21b59a0600f4',
        array []::char[]);

update selve_vedtaksperiode_generasjon set tilstand = 'AvsluttetUtenVedtak', spleis_behandling_id='1441a54c-f24c-4bcb-9a6f-82b9eb878526' where id=18989031;
insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, opprettet_av_hendelse,
                                             tilstand_endret_tidspunkt, fom, tom, skjæringstidspunkt, tilstand,
                                             spleis_behandling_id, tags)
values ('9a6fdb96-08c2-44e7-8cc9-d9f4f3ef9eda',
        'b3e86f32-73f5-43e5-9558-9acd334ebc99',
        'a8bf5796-8b53-4ff1-ae54-dc0fbce2d416',
        now(),
        '2024-03-09',
        '2024-03-18',
        '2024-03-05',
        'AvsluttetUtenVedtakMedVarsler',
        '81114968-993c-4f62-b0a4-d1f3dcac10e8',
        array []::char[]);
update selve_varsel set generasjon_ref=(select id from selve_vedtaksperiode_generasjon where unik_id='9a6fdb96-08c2-44e7-8cc9-d9f4f3ef9eda')
where vedtaksperiode_id='b3e86f32-73f5-43e5-9558-9acd334ebc99';
