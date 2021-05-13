CREATE TABLE warnings
(
    id              SERIAL PRIMARY KEY,
    message         TEXT NOT NULL,
    spleisbehov_ref UUID REFERENCES spleisbehov (id)
)
