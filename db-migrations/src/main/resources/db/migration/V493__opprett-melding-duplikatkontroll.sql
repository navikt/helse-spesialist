CREATE TABLE melding_duplikatkontroll (
    melding_id          UUID        NOT NULL,
    type                VARCHAR(64) NOT NULL,
    behandlet_tidspunkt TIMESTAMP DEFAULT now(),
    PRIMARY KEY (melding_id)
);