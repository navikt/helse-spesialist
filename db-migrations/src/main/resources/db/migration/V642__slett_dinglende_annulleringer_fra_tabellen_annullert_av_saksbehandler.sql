DELETE FROM annullert_av_saksbehandler
WHERE (migreringsstatus IS NULL OR migreringsstatus != 'OPPDATERT_MED_VEDTAKSPERIODEID') AND vedtaksperiode_id IS NULL;
