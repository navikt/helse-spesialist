INSERT INTO ukoblede_annulleringer (id,
                                    annullert_tidspunkt,
                                    saksbehandler_ref,
                                    årsaker,
                                    begrunnelse_ref,
                                    arbeidsgiver_fagsystem_id,
                                    person_fagsystem_id)
SELECT id,
       annullert_tidspunkt,
       saksbehandler_ref,
       årsaker,
       begrunnelse_ref,
       arbeidsgiver_fagsystem_id,
       person_fagsystem_id
FROM annullert_av_saksbehandler
WHERE migreringsstatus IS NULL
  AND vedtaksperiode_id IS NULL;
