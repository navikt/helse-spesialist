alter table oppgave drop column mottaker;
drop type if exists mottakertype;

ALTER TABLE oppgave ADD COLUMN mottaker VARCHAR;
ALTER TABLE oppgave ADD COLUMN type VARCHAR;
ALTER TABLE oppgave ADD COLUMN inntektskilde VARCHAR;
ALTER TABLE oppgave ADD COLUMN inntektsforhold VARCHAR;
ALTER TABLE oppgave ADD COLUMN periodetype VARCHAR;

