ALTER TABLE avslag RENAME TO vedtak_begrunnelse;
CREATE VIEW avslag AS SELECT * FROM vedtak_begrunnelse;
