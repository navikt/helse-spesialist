DROP TABLE warnings;
CREATE TABLE warning
(
    id              SERIAL PRIMARY KEY,
    message         TEXT NOT NULL,
    spleisbehov_ref UUID REFERENCES spleisbehov (id)
);
