CREATE TABLE command
(
    id           SERIAL PRIMARY KEY,
    macro_type   VARCHAR(128) NOT NULL,
    command_type VARCHAR(128) NOT NULL,
    resultat     VARCHAR(128) NOT NULL
)
