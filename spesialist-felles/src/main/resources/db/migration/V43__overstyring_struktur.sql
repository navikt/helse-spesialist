DELETE
FROM overstyrtdag;
DELETE
FROM overstyring;

ALTER TABLE overstyring
    ADD COLUMN tidspunkt        TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN person_ref       INT         NOT NULL REFERENCES person (id),
    ADD COLUMN arbeidsgiver_ref INT         NOT NULL REFERENCES arbeidsgiver (id),
    DROP COLUMN fodselsnummer,
    DROP COLUMN organisasjonsnummer;

ALTER TABLE overstyrtdag
    ALTER COLUMN grad DROP NOT NULL;
