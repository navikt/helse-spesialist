CREATE TABLE ukoblede_annulleringer
(
    id                        bigint primary key,
    annullert_tidspunkt       timestamp not null,
    saksbehandler_ref         uuid      not null references saksbehandler,
    årsaker                   character varying[],
    begrunnelse_ref           bigint,
    arbeidsgiver_fagsystem_id varchar,
    person_fagsystem_id       varchar,
    migreringsstatus          varchar   not null default 'MANGLER_KOBLING_TIL_VEDTAKSPERIODE'
);

INSERT INTO ukoblede_annulleringer (id, annullert_tidspunkt, saksbehandler_ref, årsaker, begrunnelse_ref,
                                    arbeidsgiver_fagsystem_id, person_fagsystem_id, migreringsstatus)
SELECT id,
       annullert_tidspunkt,
       saksbehandler_ref,
       årsaker,
       begrunnelse_ref,
       arbeidsgiver_fagsystem_id,
       person_fagsystem_id,
       migreringsstatus
FROM annullert_av_saksbehandler
WHERE migreringsstatus != 'OPPDATERT_MED_VEDTAKSPERIODEID';