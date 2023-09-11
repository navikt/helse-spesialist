-- Fjern alle feilaktige tildelinger
WITH feilaktig_tildelt AS (
    SELECT o.id FROM totrinnsvurdering tv
                         INNER JOIN vedtak v on tv.vedtaksperiode_id = v.vedtaksperiode_id
                         INNER JOIN oppgave o on v.id = o.vedtak_ref
                         INNER JOIN tildeling t on o.id = t.oppgave_id_ref
    WHERE o.status = 'AvventerSaksbehandler' AND (
            (t.saksbehandler_ref = tv.saksbehandler AND er_retur = false) OR
            (t.saksbehandler_ref = tv.beslutter AND er_retur = true)
        )
      AND utbetaling_id_ref IS NULL
)
DELETE FROM tildeling WHERE oppgave_id_ref IN (SELECT id FROM feilaktig_tildelt);

-- Tildel til beslutter
DO
$$
    BEGIN
        IF EXISTS (SELECT 1 FROM saksbehandler WHERE oid IN (
             'ed50903c-3776-4037-ba17-ce4798346ff3', '3cac4e6a-56d0-4c9a-a539-7fe674f25c4e', '2eccf7c2-4015-4e21-9ef4-108464e8944b'
        )) THEN
            INSERT INTO tildeling(saksbehandler_ref, oppgave_id_ref, på_vent) VALUES ('ed50903c-3776-4037-ba17-ce4798346ff3', 3098753, false);
            INSERT INTO tildeling(saksbehandler_ref, oppgave_id_ref, på_vent) VALUES ('3cac4e6a-56d0-4c9a-a539-7fe674f25c4e', 3096236, false);
            INSERT INTO tildeling(saksbehandler_ref, oppgave_id_ref, på_vent) VALUES ('2eccf7c2-4015-4e21-9ef4-108464e8944b', 3097625, false);
        END IF;
    END
$$;

-- Tildel til saksbehandler
DO
$$
    BEGIN
        IF EXISTS (SELECT 1 FROM saksbehandler WHERE oid IN (
            'fd395747-64b7-4247-a8c6-214d17d6c42f', '40c95f8d-e418-4eb6-9915-12b52c0368d3'
        )) THEN
            INSERT INTO tildeling(saksbehandler_ref, oppgave_id_ref, på_vent) VALUES ('fd395747-64b7-4247-a8c6-214d17d6c42f', 3097880, false);
            INSERT INTO tildeling(saksbehandler_ref, oppgave_id_ref, på_vent) VALUES ('40c95f8d-e418-4eb6-9915-12b52c0368d3', 3098617, false);
        END IF;
    END
$$;