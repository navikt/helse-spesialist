-- Legger til kobling til hendelse
ALTER TABLE overstyring RENAME COLUMN hendelse_id TO hendelse_ref;
ALTER TABLE overstyring ADD FOREIGN KEY(hendelse_ref) REFERENCES hendelse(id);

-- Legger til rette å lagre hendelse_id for eventet vi sender til spleis. hendelse_ref gjelder internt event.
ALTER TABLE overstyring ADD COLUMN ekstern_hendelse_id UUID;

-- Følger samme navnemønster på alle tabellene
ALTER TABLE overstyrtdag RENAME TO overstyring_dag;

-- Legg til overstyring_ref
ALTER TABLE overstyring_arbeidsforhold
    ADD COLUMN overstyring_ref BIGINT REFERENCES overstyring(id);
ALTER TABLE overstyring_inntekt
    ADD COLUMN overstyring_ref BIGINT REFERENCES overstyring(id);

-- Migrerer data
ALTER TABLE overstyring
    ADD COLUMN tmp_arbeidsforhold_id BIGINT,
    ADD COLUMN tmp_inntekt_id BIGINT;

WITH arbeidsforhold_ids AS (
    INSERT INTO overstyring (begrunnelse, tidspunkt, person_ref, arbeidsgiver_ref, hendelse_ref, saksbehandler_ref, tmp_arbeidsforhold_id)
    SELECT begrunnelse, tidspunkt, person_ref, arbeidsgiver_ref, hendelse_ref, saksbehandler_ref, id
    FROM overstyring_arbeidsforhold
        RETURNING id, tmp_arbeidsforhold_id
)
UPDATE overstyring_arbeidsforhold oa
SET overstyring_ref = arbeidsforhold_ids.id
FROM arbeidsforhold_ids
WHERE oa.id = arbeidsforhold_ids.tmp_arbeidsforhold_id;

WITH inntekt_ids AS (
    INSERT INTO overstyring (begrunnelse, tidspunkt, person_ref, arbeidsgiver_ref, hendelse_ref, saksbehandler_ref, tmp_inntekt_id)
    SELECT begrunnelse, tidspunkt, person_ref, arbeidsgiver_ref, hendelse_ref, saksbehandler_ref, id
    FROM overstyring_inntekt
        RETURNING id, tmp_inntekt_id
)
UPDATE overstyring_inntekt oi
SET overstyring_ref = inntekt_ids.id
FROM inntekt_ids
WHERE oi.id = inntekt_ids.tmp_inntekt_id;

ALTER TABLE overstyring
    DROP COLUMN tmp_arbeidsforhold_id,
    DROP COLUMN tmp_inntekt_id;

-- Setter overstyring_ref NOT NULL
ALTER TABLE overstyring_arbeidsforhold
    ALTER COLUMN overstyring_ref SET NOT NULL;
ALTER TABLE overstyring_inntekt
    ALTER COLUMN overstyring_ref SET NOT NULL;

-- Sletter migrerte felter
ALTER TABLE overstyring_arbeidsforhold
    DROP COLUMN begrunnelse,
    DROP COLUMN tidspunkt,
    DROP COLUMN person_ref,
    DROP COLUMN arbeidsgiver_ref,
    DROP COLUMN hendelse_ref,
    DROP COLUMN saksbehandler_ref;
ALTER TABLE overstyring_inntekt
    DROP COLUMN begrunnelse,
    DROP COLUMN tidspunkt,
    DROP COLUMN person_ref,
    DROP COLUMN arbeidsgiver_ref,
    DROP COLUMN hendelse_ref,
    DROP COLUMN saksbehandler_ref;