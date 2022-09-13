-- Legger til eget felt for saksbehandler_oid for beslutter
ALTER TABLE oppgave
    ADD COLUMN beslutter_saksbehandler_oid UUID;

-- Fikser kolonnenavn
ALTER TABLE oppgave
    RENAME COLUMN totrinnsvurdering TO er_totrinnsoppgave;
ALTER TABLE oppgave
    RENAME COLUMN er_retur_oppgave TO er_returoppgave;
ALTER TABLE oppgave
    RENAME COLUMN er_beslutter_oppgave TO er_beslutteroppgave;

--- Migrerer beslutter_saksbehandler_oid
UPDATE oppgave
SET beslutter_saksbehandler_oid = tidligere_saksbehandler_oid
WHERE er_returoppgave = true;

UPDATE oppgave
SET beslutter_saksbehandler_oid = t.saksbehandler_ref
FROM tildeling t
WHERE id = t.oppgave_id_ref AND er_beslutteroppgave = true;

-- Migrere når vi ikke lenger flipper er_totrinnsoppgave når vi sender til beslutter
UPDATE oppgave
SET er_totrinnsoppgave = true
WHERE (er_returoppgave = true OR er_beslutteroppgave = true);