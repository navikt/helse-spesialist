ALTER TABLE kommentarer ADD COLUMN dialog_ref BIGINT;
ALTER TABLE notat ADD COLUMN dialog_ref BIGINT;
ALTER TABLE periodehistorikk ADD COLUMN dialog_ref BIGINT;
ALTER TABLE pa_vent ADD COLUMN dialog_ref BIGINT;