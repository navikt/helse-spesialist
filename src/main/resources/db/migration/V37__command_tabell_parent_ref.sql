ALTER TABLE command
    ADD COLUMN parent_ref INT REFERENCES command (id);

ALTER TABLE command
    DROP COLUMN macro_type;
ALTER TABLE command
    DROP COLUMN resultat;
