ALTER TABLE speil_snapshot ADD COLUMN versjon INT NOT NULL DEFAULT 0;

CREATE TABLE global_snapshot_versjon (
    id INT PRIMARY KEY NOT NULL,
    versjon INT NOT NULL,
    sist_endret timestamp NOT NULL default now()
);

INSERT INTO global_snapshot_versjon (id, versjon, sist_endret) VALUES (1, 0, now());
