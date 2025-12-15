ALTER TABLE totrinnsvurdering ADD COLUMN saksbehandler_ident varchar REFERENCES saksbehandler(ident) DEFAULT null;
ALTER TABLE totrinnsvurdering ADD COLUMN beslutter_ident varchar REFERENCES saksbehandler(ident) DEFAULT null;
