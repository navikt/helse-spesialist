CREATE TABLE command_context
(
    id             SERIAL PRIMARY KEY,
    context_id     UUID        NOT NULL,
    spleisbehov_id UUID        NOT NULL REFERENCES spleisbehov (id),
    opprettet      TIMESTAMP   NOT NULL DEFAULT NOW(),
    tilstand       VARCHAR(64) NOT NULL,
    data           JSON        NOT NULL
);
