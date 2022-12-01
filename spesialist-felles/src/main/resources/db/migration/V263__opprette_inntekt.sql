CREATE TABLE inntekt
(
    id                    BIGSERIAL PRIMARY KEY,
    person_ref            BIGINT NOT NULL REFERENCES person(id),
    skjaeringstidspunkt   DATE NOT NULL,
    inntekter             JSON NOT NULL,
    UNIQUE (person_ref, skjaeringstidspunkt)
);
