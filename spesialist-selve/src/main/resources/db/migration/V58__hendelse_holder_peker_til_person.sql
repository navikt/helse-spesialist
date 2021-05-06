ALTER TABLE spleisbehov ADD COLUMN fodselsnummer BIGINT;

UPDATE spleisbehov sb SET fodselsnummer = p.fodselsnummer FROM person p WHERE p.id = (
    SELECT person_ref FROM vedtak v WHERE v.vedtaksperiode_id = sb.spleis_referanse
    );
