alter table oppdrag drop column if exists endringskode;
alter table oppdrag drop column if exists sisteArbeidsgiverdag;
alter table oppdrag drop column if exists fagområde;
alter table utbetalingslinje drop column if exists delytelseid;
alter table utbetalingslinje drop column if exists refdelytelseid;
alter table utbetalingslinje drop column if exists reffagsystemid;
alter table utbetalingslinje drop column if exists endringskode;
alter table utbetalingslinje drop column if exists klassekode;
alter table utbetalingslinje drop column if exists statuskode;
alter table utbetalingslinje drop column if exists datostatusfom;
alter table utbetalingslinje drop column if exists dagsats;
alter table utbetalingslinje drop column if exists lønn;
alter table utbetalingslinje drop column if exists grad;

drop type if exists oppdrag_endringskode;
drop type if exists oppdrag_klassekode;
drop type if exists oppdrag_statuskode;
drop type if exists oppdrag_fagområde;