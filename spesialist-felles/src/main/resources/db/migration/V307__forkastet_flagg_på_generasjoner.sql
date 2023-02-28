ALTER TABLE vedtak ADD COLUMN forkastet BOOLEAN;
ALTER TABLE vedtak ADD COLUMN forkastet_tidspunkt timestamp;
ALTER TABLE vedtak ADD COLUMN forkastet_av_hendelse uuid;