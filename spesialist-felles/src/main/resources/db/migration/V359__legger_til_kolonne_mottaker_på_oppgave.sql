CREATE TYPE mottakertype AS ENUM('SYKMELDT', 'ARBEIDSGIVER', 'BEGGE');
ALTER TABLE oppgave ADD COLUMN mottaker mottakertype DEFAULT NULL;

WITH aapne_oppgaver AS (
    SELECT o.id, o.vedtak_ref, v.vedtaksperiode_id, o.utbetaling_id, v.person_ref
    FROM oppgave o, vedtak v
    WHERE o.status='AvventerSaksbehandler' AND o.vedtak_ref=v.id
), mottaker_paa_oppgave AS (
    SELECT ao.id,
           CASE WHEN SUM(ABS(arbeidsgiverbeløp)) > 0 AND SUM(ABS(personbeløp)) > 0 THEN 'BEGGE'
                WHEN SUM(ABS(personbeløp)) > 0 THEN 'SYKMELDT'
                WHEN SUM(ABS(arbeidsgiverbeløp)) > 0 THEN 'ARBEIDSGIVER'
           END AS mottaker
    FROM utbetaling_id ui, aapne_oppgaver ao
    WHERE ui.person_ref=ao.person_ref AND
            ui.utbetaling_id IN (
            SELECT utbetaling_id
            FROM selve_vedtaksperiode_generasjon
            WHERE skjæringstidspunkt=(
                SELECT skjæringstidspunkt
                FROM selve_vedtaksperiode_generasjon
                WHERE vedtaksperiode_id=ao.vedtaksperiode_id AND tilstand='Ulåst'
            ) AND tilstand='Ulåst'
        )
    GROUP BY ao.id
)
UPDATE oppgave o
    SET mottaker=cast(mpo.mottaker as mottakertype)
    FROM mottaker_paa_oppgave mpo
    WHERE o.id=mpo.id;




