alter table oppdrag
alter column sisteArbeidsgiverdag type varchar(32);

insert into oppdrag (id, fagsystem_id, mottaker, fagområde, endringskode, sistearbeidsgiverdag)
select distinct on (u.arbeidsgiver_fagsystem_id_ref) u.arbeidsgiver_fagsystem_id_ref as id,
     r."fagsystemId",r.mottaker,r.fagområde,r.endringskode,r."sisteArbeidsgiverdag"
from utbetaling as u,
     json_populate_record(null::oppdrag_type, data -> 'arbeidsgiverOppdrag') as r
on conflict(id) do update
    set mottaker             = EXCLUDED.mottaker,
        fagområde            = EXCLUDED.fagområde,
        endringskode         = EXCLUDED.endringskode,
        sisteArbeidsgiverdag = EXCLUDED.sisteArbeidsgiverdag
;

insert into oppdrag (id, fagsystem_id, mottaker, fagområde, endringskode, sistearbeidsgiverdag)
select distinct on (u.arbeidsgiver_fagsystem_id_ref) u.arbeidsgiver_fagsystem_id_ref as id,
     r."fagsystemId",r.mottaker,r.fagområde,r.endringskode,r."sisteArbeidsgiverdag"
from utbetaling as u,
     json_populate_record(null::oppdrag_type, data -> 'personOppdrag') as r
on conflict(id) do update
    set mottaker             = EXCLUDED.mottaker,
        fagområde            = EXCLUDED.fagområde,
        endringskode         = EXCLUDED.endringskode,
        sisteArbeidsgiverdag = EXCLUDED.sisteArbeidsgiverdag
;

update oppdrag set sisteArbeidsgiverdag = NULL WHERE sisteArbeidsgiverdag = '-999999999-01-01';
