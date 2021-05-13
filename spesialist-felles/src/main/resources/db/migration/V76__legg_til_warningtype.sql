CREATE TYPE warning_kilde AS ENUM('Spleis', 'Spesialist');
ALTER TABLE warning ADD COLUMN kilde warning_kilde NOT NULL DEFAULT 'Spleis'::warning_kilde;
ALTER TABLE warning ALTER COLUMN kilde DROP DEFAULT;
