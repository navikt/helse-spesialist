CREATE TABLE person_som_skal_holdes_igjen(
    fodselsnummer VARCHAR PRIMARY KEY NOT NULL,
    opprettet TIMESTAMP NOT NULL,
    oider_som_kan_sla_opp uuid[] NOT NULL DEFAULT []
);
