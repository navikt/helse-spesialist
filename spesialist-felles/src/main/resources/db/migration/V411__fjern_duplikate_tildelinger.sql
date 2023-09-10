DELETE FROM tildeling T1 USING tildeling T2
WHERE T1.ctid < T2.ctid                                -- slett "eldre" rader
AND T1.oppgave_id_ref = T2.oppgave_id_ref;             -- list kolonner som definerer duplikater

ALTER TABLE tildeling ADD PRIMARY KEY (oppgave_id_ref);