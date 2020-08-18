ALTER TABLE overstyring
    ADD COLUMN hendelse_id UUID NOT NULL DEFAULT '0d7388cc-e8d2-4adf-a571-da443d68e774'::uuid;

ALTER TABLE overstyring
    ALTER COLUMN hendelse_id DROP DEFAULT
