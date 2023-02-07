-- Dupliserer eksisterende data til de nye feltene
UPDATE overstyring_inntekt oi
    SET arbeidsgiver_ref = o.arbeidsgiver_ref, begrunnelse = o.begrunnelse
    FROM overstyring o
    WHERE oi.overstyring_ref = o.id;

UPDATE overstyring_arbeidsforhold oa
    SET arbeidsgiver_ref = o.arbeidsgiver_ref, begrunnelse = o.begrunnelse
    FROM overstyring o
    WHERE oa.overstyring_ref = o.id;

-- Setter de nye feltene til NOT NULL
ALTER TABLE overstyring_inntekt
    ALTER COLUMN arbeidsgiver_ref SET NOT NULL,
    ALTER COLUMN begrunnelse SET NOT NULL;

ALTER TABLE overstyring_arbeidsforhold
    ALTER COLUMN arbeidsgiver_ref SET NOT NULL,
    ALTER COLUMN begrunnelse SET NOT NULL;
