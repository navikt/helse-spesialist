-- Dersom en saksbehandler legger inn eksempelvis notat på feil person så trengs en mulighet for å markere at det ikke skal gjelde

ALTER TABLE notat
    ADD COLUMN feilregistrert BOOLEAN DEFAULT false NOT NULL;
