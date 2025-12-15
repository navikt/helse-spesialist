ALTER TABLE saksbehandler ALTER COLUMN ident SET NOT NULL;
ALTER TABLE saksbehandler ADD UNIQUE(ident);
