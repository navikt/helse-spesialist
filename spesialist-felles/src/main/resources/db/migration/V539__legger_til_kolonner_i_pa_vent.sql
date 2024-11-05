alter table pa_vent add column årsaker varchar[] not null default array[]::varchar[];
alter table pa_vent add column notattekst varchar;

update pa_vent pv set notattekst = n.tekst from notat n where pv.dialog_ref = n.dialog_ref;