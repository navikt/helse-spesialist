/*
 På et tidspunkt ble kolonnen utbetaling_id innført i oppgave. Den måtte være nullable pga. at det ikke var
 gjennomførbart å backfille kolonnen for historiske oppgaver.

 Det skaper trøbbel når forretningslogikken antar at utbetaling_id er satt. Derfor flyttet vi alle problematiske data
 til arkiv-tabeller, og gjør kolonnen not null.
 */

create table arkiv_tildeling_for_oppgaver_uten_utbetaling_id as table tildeling;
create table arkiv_oppgave_uten_utbetaling_id as table oppgave;

delete from arkiv_tildeling_for_oppgaver_uten_utbetaling_id t using oppgave
where oppgave.id = t.oppgave_id_ref and oppgave.utbetaling_id is not null;

delete from tildeling t using oppgave
where oppgave.id = t.oppgave_id_ref and oppgave.utbetaling_id is null;

delete from arkiv_oppgave_uten_utbetaling_id
where utbetaling_id is not null;

delete from oppgave
where utbetaling_id is null;

alter table oppgave alter column utbetaling_id set not null;
