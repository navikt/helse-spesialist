ALTER TABLE periodehistorikk ADD generasjon_id uuid REFERENCES selve_vedtaksperiode_generasjon(unik_id);
ALTER TABLE periodehistorikk ALTER column utbetaling_id DROP NOT NULL;

CREATE INDEX ON periodehistorikk(generasjon_id);
