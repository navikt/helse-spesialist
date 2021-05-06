CREATE TABLE annullert_av_saksbehandler(
    id                   SERIAL         NOT NULL primary key,
    annullert_tidspunkt  TIMESTAMP      NOT NULL,
    saksbehandler_ref    UUID           NOT NULL REFERENCES saksbehandler (oid)
);

ALTER TABLE utbetaling ADD COLUMN annullert_av_saksbehandler_ref BIGINT;
ALTER TABLE utbetaling ADD FOREIGN KEY(annullert_av_saksbehandler_ref) REFERENCES annullert_av_saksbehandler(id);
