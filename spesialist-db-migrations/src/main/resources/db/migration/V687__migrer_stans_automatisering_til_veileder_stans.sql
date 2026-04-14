-- Step 1: Insert one veileder_stans row per STOPP_AUTOMATIKK event.
-- Idempotent: skips rows that already exist (matched on identitetsnummer, opprettet, original_melding_id).
INSERT INTO veileder_stans (id, identitetsnummer, arsaker, opprettet, original_melding_id)
SELECT
    gen_random_uuid(),
    sa.fødselsnummer,
    sa.årsaker::text[],
    sa.opprettet AT TIME ZONE 'UTC',
    COALESCE((sa.original_melding->>'uuid')::uuid, gen_random_uuid())
FROM stans_automatisering sa
WHERE sa.status = 'STOPP_AUTOMATIKK'
AND NOT EXISTS (
    SELECT 1
    FROM veileder_stans vs
    WHERE vs.identitetsnummer = sa.fødselsnummer
      AND vs.opprettet = sa.opprettet AT TIME ZONE 'UTC'
      AND vs.original_melding_id = COALESCE((sa.original_melding->>'uuid')::uuid, vs.original_melding_id)
);

-- Step 2: Mark opphevet stans.
-- For each STOPP_AUTOMATIKK, find the first subsequent NORMAL for the same person,
-- then set opphevet_tidspunkt on the corresponding veileder_stans row.
-- Idempotent: only updates rows where opphevet_tidspunkt IS NULL.
WITH stopp_with_first_normal AS (
    SELECT
        sa_stopp.fødselsnummer,
        sa_stopp.opprettet AS stopp_opprettet,
        MIN(sa_normal.opprettet) AS normal_opprettet
    FROM stans_automatisering sa_stopp
    JOIN stans_automatisering sa_normal
        ON sa_normal.fødselsnummer = sa_stopp.fødselsnummer
        AND sa_normal.status = 'NORMAL'
        AND sa_normal.opprettet > sa_stopp.opprettet
    WHERE sa_stopp.status = 'STOPP_AUTOMATIKK'
    GROUP BY sa_stopp.fødselsnummer, sa_stopp.opprettet
)
UPDATE veileder_stans vs
SET opphevet_tidspunkt = swn.normal_opprettet AT TIME ZONE 'UTC'
FROM stopp_with_first_normal swn
WHERE vs.identitetsnummer = swn.fødselsnummer
  AND vs.opprettet = swn.stopp_opprettet AT TIME ZONE 'UTC'
  AND vs.opphevet_tidspunkt IS NULL;

