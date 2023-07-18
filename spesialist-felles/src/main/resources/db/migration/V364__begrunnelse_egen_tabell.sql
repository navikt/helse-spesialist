CREATE TABLE begrunnelse(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    tekst TEXT NOT NULL,
    type VARCHAR,
    saksbehandler_ref uuid NOT NULL
);

ALTER TABLE skjonnsfastsetting_sykepengegrunnlag ADD COLUMN begrunnelse_ref BIGINT;
