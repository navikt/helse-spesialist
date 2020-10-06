CREATE TYPE warning_kilde AS ENUM('Spleis', 'Spesialist');
ALTER TABLE warning ADD COLUMN kilde warning_kilde;
