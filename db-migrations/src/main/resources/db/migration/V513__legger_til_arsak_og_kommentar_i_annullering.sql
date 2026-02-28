ALTER TABLE annullert_av_saksbehandler ADD COLUMN årsaker VARCHAR[];
ALTER TABLE annullert_av_saksbehandler ADD COLUMN begrunnelse_ref BIGINT;
ALTER Table annullert_av_saksbehandler ADD COLUMN vedtaksperiode_id uuid;
ALTER Table annullert_av_saksbehandler ADD COLUMN utbetaling_id uuid;