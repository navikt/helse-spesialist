CREATE TABLE notat(
      id SERIAL PRIMARY KEY,
      tekst VARCHAR(200),
      opprettet TIMESTAMP DEFAULT now(),
      saksbehandler_oid UUID,
      saksbehandler_navn VARCHAR(100),
      oppgave_ref INT
)
