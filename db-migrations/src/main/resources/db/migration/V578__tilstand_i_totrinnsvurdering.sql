create type totrinnsvurdering_tilstand as enum ('AVVENTER_SAKSBEHANDLER', 'AVVENTER_BESLUTTER', 'GODKJENT');

alter table totrinnsvurdering
    add column tilstand totrinnsvurdering_tilstand;

update totrinnsvurdering set tilstand = 'GODKJENT' where tilstand is null and utbetaling_id_ref is not null;
update totrinnsvurdering set tilstand = 'AVVENTER_SAKSBEHANDLER' where tilstand is null and er_retur = true;
update totrinnsvurdering set tilstand = 'AVVENTER_BESLUTTER' where tilstand is null and saksbehandler is not null;
update totrinnsvurdering set tilstand = 'AVVENTER_SAKSBEHANDLER' where tilstand is null;

alter table totrinnsvurdering
    alter column tilstand set not null;

alter table totrinnsvurdering drop column er_retur;