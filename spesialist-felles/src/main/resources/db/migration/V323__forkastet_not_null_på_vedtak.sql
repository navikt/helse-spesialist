UPDATE vedtak SET forkastet = false WHERE forkastet IS NULL;
ALTER TABLE vedtak ALTER COLUMN forkastet SET NOT NULL;