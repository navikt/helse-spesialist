UPDATE skjonnsfastsetting_sykepengegrunnlag
SET subsumsjon = (subsumsjon::jsonb || '{"lovverk": "folketrygdloven", "lovverksversjon": "2019-01-01"}')::json
WHERE subsumsjon->'lovverk' IS NULL AND subsumsjon->>'paragraf' = '8-30';
