TRUNCATE TABLE skjonnsfastsetting_sykepengegrunnlag;
CREATE TABLE skjonnsfastsetting_sykepengegrunnlag_arbeidsgiver (
   id                            SERIAL PRIMARY KEY,
   arlig                         numeric(12, 2) NOT NULL,
   fra_arlig                     numeric(12, 2) default NULL::numeric,
   arbeidsgiver_ref              integer NOT NULL REFERENCES arbeidsgiver(id),
   skjonnsfastsetting_sykepengegrunnlag_ref bigint not null references skjonnsfastsetting_sykepengegrunnlag(id)
);
ALTER TABLE skjonnsfastsetting_sykepengegrunnlag DROP COLUMN arlig, DROP COLUMN fra_arlig, DROP COLUMN arbeidsgiver_ref;