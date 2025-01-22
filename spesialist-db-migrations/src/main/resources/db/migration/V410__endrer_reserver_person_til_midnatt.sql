ALTER TABLE reserver_person
ALTER COLUMN gyldig_til
SET DEFAULT current_date + time '23:59:59';