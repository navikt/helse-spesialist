ALTER TABLE overstyring
    DROP COLUMN overstyrtedager;

CREATE TABLE overstyrtdag
(
    id              SERIAL PRIMARY KEY,
    overstyring_ref BIGINT references overstyring (id) NOT NULL,
    dato            DATE                               NOT NULL,
    dagtype         VARCHAR(64)                        NOT NULL,
    grad            INT                                NOT NULL
)
