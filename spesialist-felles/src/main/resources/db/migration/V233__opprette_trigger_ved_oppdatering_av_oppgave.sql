CREATE
OR REPLACE FUNCTION oppdater_oppdatert_kolonne()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.oppdatert
= now();
RETURN NEW;
END;
$$
language 'plpgsql';

CREATE TRIGGER oppdater_oppgave_oppdatert
    BEFORE UPDATE
    ON oppgave
    FOR EACH ROW EXECUTE PROCEDURE oppdater_oppdatert_kolonne();

CREATE TRIGGER oppdater_arbeidsforhold_oppdatert
    BEFORE UPDATE
    ON arbeidsforhold
    FOR EACH ROW EXECUTE PROCEDURE oppdater_oppdatert_kolonne();

CREATE TRIGGER oppdater_arbeidsgiver_bransjer_oppdatert
    BEFORE UPDATE
    ON arbeidsgiver_bransjer
    FOR EACH ROW EXECUTE PROCEDURE oppdater_oppdatert_kolonne();
