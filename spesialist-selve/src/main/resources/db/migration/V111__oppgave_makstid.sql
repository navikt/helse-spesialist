CREATE TABLE oppgave_makstid
(
    oppgave_ref        BIGINT NOT NULL REFERENCES oppgave(id) ON DELETE CASCADE PRIMARY KEY,
    tildelt            BOOLEAN DEFAULT false,
    makstid            TIMESTAMP NOT NULL DEFAULT date_trunc('day', now()) + (24*60*60 - 1) * interval '1 second' + INTERVAL '4 DAYS' -- End of day in 4 days
);
