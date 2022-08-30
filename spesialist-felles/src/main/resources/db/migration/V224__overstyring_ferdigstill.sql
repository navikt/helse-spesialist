ALTER TABLE overstyring
    ADD COLUMN ferdigstilt BOOLEAN DEFAULT true;

ALTER TABLE overstyring
    ALTER COLUMN ferdigstilt SET DEFAULT false;