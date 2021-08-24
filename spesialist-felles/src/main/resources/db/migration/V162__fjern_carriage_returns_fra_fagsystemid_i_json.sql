/* utbetaling */
-- oppdatere fagsystemId i arbeidsgiverOppdrag
UPDATE utbetaling
SET data = jsonb_set(data::jsonb, '{arbeidsgiverOppdrag,fagsystemId}', to_jsonb(regexp_replace(data->'arbeidsgiverOppdrag'->>'fagsystemId',E'[\\r\\n]+', '', 'g')), false)
WHERE data -> 'arbeidsgiverOppdrag' ->> 'fagsystemId' LIKE E'%\r\n' AND opprettet > '2021-08-05';

-- oppdatere fagsystemId i personOppdrag
UPDATE utbetaling
SET data = jsonb_set(data::jsonb, '{personOppdrag,fagsystemId}', to_jsonb(regexp_replace(data->'personOppdrag'->>'fagsystemId',E'[\\r\\n]+', '', 'g')), false)
WHERE data -> 'personOppdrag' ->> 'fagsystemId' LIKE E'%\r\n' AND opprettet > '2021-08-05';
-- oppdatere refFagsystemId i arbeidsgiverOppdrag linjer
with
    linjer as
        (select id, jsonb_array_elements((data->'arbeidsgiverOppdrag'->'linjer')::jsonb) as original_linje
        from utbetaling
        where opprettet > '2021-08-05'),
    modifiedLinjer as
        (select
             id,
             CASE
                 WHEN original_linje->>'refFagsystemId' IS NOT NULL THEN
                     jsonb_set(original_linje, '{refFagsystemId}', to_jsonb(regexp_replace(original_linje->>'refFagsystemId',E'[\\r\\n]+', '', 'g')), false)
                 ELSE
                     original_linje
                 END as modifisert_linje
         from linjer)
update utbetaling
set data = jsonb_set(data::jsonb, '{arbeidsgiverOppdrag,linjer}', (select jsonb_agg(modifisert_linje) from modifiedLinjer WHERE modifiedLinjer.id = utbetaling.id))
where utbetaling.id in (select id from linjer where original_linje ->>'refFagsystemId' LIKE E'%\r\n');

/* hendelse */
-- oppdatere fagsystemId i arbeidsgiverOppdrag
UPDATE hendelse
SET data = jsonb_set(data::jsonb, '{arbeidsgiverOppdrag,fagsystemId}', to_jsonb(regexp_replace(data->'arbeidsgiverOppdrag'->>'fagsystemId',E'[\\r\\n]+', '', 'g')), false)
WHERE type = 'UTBETALING_ENDRET' and data -> 'arbeidsgiverOppdrag' ->> 'fagsystemId' LIKE E'%\r\n' and (data->>'@opprettet')::timestamp > '2021-08-05';

-- oppdatere fagsystemId i personOppdrag
UPDATE hendelse
SET data = jsonb_set(data::jsonb, '{personOppdrag,fagsystemId}', to_jsonb(regexp_replace(data->'personOppdrag'->>'fagsystemId',E'[\\r\\n]+', '', 'g')), false)
WHERE type = 'UTBETALING_ENDRET' and data -> 'personOppdrag' ->> 'fagsystemId' LIKE E'%\r\n' and (data->>'@opprettet')::timestamp > '2021-08-05';

-- oppdatere refFagsystemId i arbeidsgiverOppdrag linjer
with
    linjer as
        (select id, jsonb_array_elements((data->'arbeidsgiverOppdrag'->'linjer')::jsonb) as original_linje
        from hendelse
        where (data->>'@opprettet')::timestamp > '2021-08-05' AND type = 'UTBETALING_ENDRET'),
    modifiedLinjer as
        (select
             id,
             CASE
                 WHEN original_linje->>'refFagsystemId' IS NOT NULL THEN
                     jsonb_set(original_linje, '{refFagsystemId}', to_jsonb(regexp_replace(original_linje->>'refFagsystemId',E'[\\r\\n]+', '', 'g')), false)
                 ELSE
                     original_linje
                 END as modifisert_linje
         from linjer)
update hendelse
set data = jsonb_set(data::jsonb, '{arbeidsgiverOppdrag,linjer}', (select jsonb_agg(modifisert_linje) from modifiedLinjer WHERE modifiedLinjer.id = hendelse.id))
where type = 'UTBETALING_ENDRET' and hendelse.id in (select id from linjer where original_linje ->>'refFagsystemId' LIKE E'%\r\n');
