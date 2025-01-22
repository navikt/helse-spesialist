WITH overstyringer_med_apne_oppgaver AS (
    SELECT overstyring_ref, v.vedtaksperiode_id
    FROM overstyringer_for_vedtaksperioder ofv
        INNER JOIN vedtak v ON ofv.vedtaksperiode_id = v.vedtaksperiode_id
        INNER JOIN overstyring o ON ofv.overstyring_ref=o.id
    WHERE v.id in (SELECT vedtak_ref FROM oppgave WHERE status='AvventerSaksbehandler')
        AND o.ferdigstilt=FALSE AND o.vedtaksperiode_id IS NULL)
UPDATE overstyring
SET vedtaksperiode_id=omao.vedtaksperiode_id
FROM overstyringer_med_apne_oppgaver omao
WHERE id=omao.overstyring_ref;

UPDATE overstyring
SET vedtaksperiode_id='00000000-0000-0000-aaaa-bbbbbbbbbbbb'
WHERE vedtaksperiode_id is null;

ALTER TABLE overstyring ALTER COLUMN vedtaksperiode_id SET NOT NULL;
DROP TRIGGER sjekk_vedtaksperiode_id_not_null ON overstyring;