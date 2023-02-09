WITH overstyring_refs AS (SELECT DISTINCT overstyring_ref FROM overstyring_dag),
    overstyring_tidslinje_rader AS (
        INSERT INTO overstyring_tidslinje (overstyring_ref, arbeidsgiver_ref, begrunnelse)
            SELECT overstyring_refs.overstyring_ref, o.arbeidsgiver_ref, o.begrunnelse
            FROM overstyring o
            JOIN overstyring_refs ON overstyring_refs.overstyring_ref = o.id
            RETURNING id, overstyring_ref
    )

UPDATE overstyring_dag od
SET overstyring_tidslinje_ref = overstyring_tidslinje_rader.id
FROM overstyring_tidslinje_rader
WHERE od.overstyring_ref = overstyring_tidslinje_rader.overstyring_ref;
