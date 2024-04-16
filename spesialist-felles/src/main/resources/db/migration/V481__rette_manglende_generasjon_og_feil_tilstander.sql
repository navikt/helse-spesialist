insert into selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, opprettet_av_hendelse,
                                             tilstand_endret_tidspunkt, fom, tom, skjæringstidspunkt, tilstand,
                                             spleis_behandling_id, tags)
values ('aabbccdd-bb24-4d7f-8122-f353203ea5f1',
        '5e0c7919-19dc-4a4f-9473-abf261411526',
        'aabbccdd-bb24-4d7f-8122-f353203ea5f1',
        now(),
        '2023-12-18',
        '2023-12-22',
        '2023-12-18',
        'VedtakFattet',
        '4aeabec2-7015-488e-b732-7d598c9a809c',
        array []::char[]);
update selve_vedtaksperiode_generasjon set tilstand = 'KlarTilBehandling'
where spleis_behandling_id = '02d18814-aba8-4003-b696-2b5861b160a9';

--

update selve_vedtaksperiode_generasjon set tilstand = 'KlarTilBehandling'
where spleis_behandling_id = '7b06d5a3-3423-48d4-9aa2-c30710a60749';

--

update selve_vedtaksperiode_generasjon set tilstand = 'KlarTilBehandling'
where spleis_behandling_id = 'c50f0691-7ea1-4a3c-9e2f-ac467278f513';

--

update selve_vedtaksperiode_generasjon set tilstand = 'KlarTilBehandling'
where spleis_behandling_id = '74f33d64-2bd7-4ffb-94d9-8f0867259fa3';

--

update selve_vedtaksperiode_generasjon set tilstand = 'KlarTilBehandling'
where spleis_behandling_id = '6add6844-a686-471c-a40e-59946d5b9e1a';
