DELETE FROM overstyrt_vedtaksperiode;

CREATE TYPE overstyringtype AS ENUM('Inntekt', 'Dager', 'Arbeidsforhold');

ALTER TABLE overstyrt_vedtaksperiode
    ADD COLUMN type overstyringtype NOT NULL DEFAULT 'Inntekt'::overstyringtype;
ALTER TABLE overstyrt_vedtaksperiode
    ALTER COLUMN type DROP DEFAULT;

ALTER TABLE overstyrt_vedtaksperiode
    ADD COLUMN ferdigstilt BOOLEAN NOT NULL DEFAULT FALSE;
