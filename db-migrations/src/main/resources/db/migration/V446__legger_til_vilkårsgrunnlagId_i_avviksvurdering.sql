ALTER TABLE avviksvurdering ADD CONSTRAINT unique_avviksvurdering_unik_id UNIQUE(unik_id);

CREATE TABLE vilkarsgrunnlag_per_avviksvurdering
(
    avviksvurdering_ref UUID NOT NULL REFERENCES avviksvurdering(unik_id) ON DELETE CASCADE,
    vilkårsgrunnlag_id  UUID NOT NULL,
    PRIMARY KEY (avviksvurdering_ref, vilkårsgrunnlag_id)
);

