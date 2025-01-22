insert into dialog (opprettet)
select opprettet from notat where dialog_ref is null;

update notat n set dialog_ref = d.id from dialog d where n.opprettet = d.opprettet;

update kommentarer k set dialog_ref = n.dialog_ref from notat n where n.id = k.notat_ref;

update periodehistorikk ph set dialog_ref = n.dialog_ref from notat n where n.id = ph.notat_id;

