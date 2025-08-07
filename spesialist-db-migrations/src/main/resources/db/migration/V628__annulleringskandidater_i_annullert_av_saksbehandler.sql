create table annulleringskandidater_annullert_av_saksbehandler
(
    vedtaksperiode_id              uuid primary key,
    annullert_av_saksbehandler_ref bigint references annullert_av_saksbehandler (id) ON DELETE CASCADE
);