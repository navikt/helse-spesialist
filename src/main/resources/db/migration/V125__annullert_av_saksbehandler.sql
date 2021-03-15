CREATE TABLE annullert_av_saksbehandler(
    id                   SERIAL          NOT NULL CONSTRAINT annullert_av_saksbehnalder_pkey primary key,
    annullert_tidspunkt  DATE           NOT NULL,
    saksbehandler_ref    UUID           NOT NULL REFERENCES saksbehandler (oid)
);

ALTER TABLE utbetaling ADD CONSTRAINT annullert_av_saksbehandler_ref_fkey FOREIGN KEY(id) REFERENCES annullert_av_saksbehandler (id);
