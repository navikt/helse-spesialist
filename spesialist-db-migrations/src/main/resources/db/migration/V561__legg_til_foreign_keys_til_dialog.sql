ALTER TABLE notat
    ADD CONSTRAINT notat_dialog_ref_fkey
        FOREIGN KEY (dialog_ref) REFERENCES dialog(id);
ALTER TABLE notat
    ADD CONSTRAINT notat_unique_constraint_dialog_ref UNIQUE (dialog_ref);

ALTER TABLE periodehistorikk
    ADD CONSTRAINT periodehistorikk_dialog_ref_fkey
        FOREIGN KEY (dialog_ref) REFERENCES dialog(id);
ALTER TABLE periodehistorikk
    ADD CONSTRAINT periodehistorikk_unique_constraint_dialog_ref UNIQUE (dialog_ref);

ALTER TABLE pa_vent
    ADD CONSTRAINT pa_vent_dialog_ref_fkey
        FOREIGN KEY (dialog_ref) REFERENCES dialog(id);
ALTER TABLE pa_vent
    ADD CONSTRAINT pa_vent_unique_constraint_dialog_ref UNIQUE (dialog_ref);

ALTER TABLE kommentarer
    ADD CONSTRAINT kommentarer_dialog_ref_fkey
        FOREIGN KEY (dialog_ref) REFERENCES dialog(id) ON DELETE CASCADE;
