ALTER TABLE overstyring_inntekt
    ADD COLUMN fra_manedlig_inntekt NUMERIC(12, 2);

ALTER TABLE overstyring_dag
    ADD COLUMN fra_dagtype VARCHAR(64),
    ADD COLUMN fra_grad    INT;
