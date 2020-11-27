
insert into utbetalingslinje (oppdrag_id, delytelseId, refDelytelseId, refFagsystemId, endringskode,
                              klassekode, statuskode, datoStatusFom, fom, tom, dagsats, lønn, grad)
select u.arbeidsgiver_fagsystem_id_ref,r."delytelseId",r."refDelytelseId",r."refFagsystemId",r.endringskode,r.klassekode,
       r.statuskode,r."datoStatusFom",r.fom,r.tom,r.dagsats,r.lønn,r.grad
from utbetaling u,
     json_populate_recordset(null::utbetalingslinje_type, data -> 'arbeidsgiverOppdrag' -> 'linjer') as r
;

