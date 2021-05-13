ALTER TABLE spleisbehov ADD COLUMN spleis_referanse UUID;
UPDATE spleisbehov SET spleis_referanse=(data ->> 'vedtaksperiodeId')::UUID;
ALTER TABLE spleisbehov ALTER COLUMN spleis_referanse SET NOT NULL;
UPDATE spleisbehov SET data=data::jsonb - 'vedtaksperiodeId'
