ALTER TABLE saksbehandleroppgavetype ADD COLUMN inntektskilde VARCHAR(64) NOT NULL DEFAULT 'EN_ARBEIDSGIVER';
ALTER TABLE saksbehandleroppgavetype ALTER COLUMN inntektskilde DROP DEFAULT;
