CREATE TABLE totrinnsvurdering
(
    id                  SERIAL PRIMARY KEY,
    person_ref          INT references person (id)       NOT NULL,
    arbeidsgiver_ref    INT references arbeidsgiver (id) NOT NULL,
    skjaeringstidspunkt DATE                             NOT NULL,
    er_retur            BOOLEAN default false            NOT NULL,
    saksbehandler       UUID references saksbehandler (oid),
    beslutter           UUID references saksbehandler (oid)
);