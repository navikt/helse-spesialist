CREATE TABLE infotrygdutbetalinger
(
    id   INT UNIQUE NOT NULL,
    data JSON NOT NULL
);

ALTER TABLE person ADD COLUMN infotrygdutbetalinger_ref INT REFERENCES infotrygdutbetalinger (id);
ALTER TABLE person ADD COLUMN infotrygdutbetalinger_oppdatert DATE NOT NULL DEFAULT now();
