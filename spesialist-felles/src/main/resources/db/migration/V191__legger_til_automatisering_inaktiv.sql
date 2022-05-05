--- AUTOMATISERING
ALTER TABLE automatisering
    DROP CONSTRAINT automatisering_pkey;

ALTER TABLE automatisering
    ADD COLUMN id SERIAL PRIMARY KEY;
ALTER TABLE automatisering
    ADD COLUMN inaktiv_fra timestamp;

--- AUTOMATISERING PROBLEM
ALTER TABLE automatisering_problem
    ADD COLUMN inaktiv_fra timestamp;
