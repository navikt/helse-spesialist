CREATE TABLE poison_pill
(
    identifikator varchar   NOT NULL,
    feltnavn      varchar   NOT NULL,
    opprettet     timestamp NOT NULL DEFAULT now()
)