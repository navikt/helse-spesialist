-- Saker som noen andre har tatt, men ikke lagt på vent:
delete from tildeling where oppgave_id_ref in (
    2889536,
    2890699,
    2891391,
    2892127,
    2892503,
    2892629
);

with duplikate_oppgaver as (
    select utbetaling_id
    from oppgave o
    group by utbetaling_id
    having count(1) > 1
), ny_oppgave as (
    select max(id) as id
    from oppgave
             join duplikate_oppgaver using (utbetaling_id)
    group by utbetaling_id
), opprinnelig_oppgave as (
    select min(id) as id
    from oppgave
             join duplikate_oppgaver using (utbetaling_id)
    group by utbetaling_id
), de_som_var_på_vent as (
    select * from opprinnelig_oppgave
                      join oppgave using (id)
                      join tildeling t on oppgave.id = t.oppgave_id_ref and på_vent = true
), de_som_er_lagt_på_vent as (
    select id from ny_oppgave
                       inner join tildeling on tildeling.oppgave_id_ref = ny_oppgave.id and på_vent = true
    group by id
), de_som_skal_gjenopprettes as (
    select *
    from de_som_var_på_vent
    where utbetaling_id not in (select oppgave.utbetaling_id from oppgave join de_som_er_lagt_på_vent using (id))
), oppgaveid_som_skal_tildeles as (
    select utbetaling_id, oppgave.id
    from oppgave
             join ny_oppgave using (id)
             inner join de_som_skal_gjenopprettes using (utbetaling_id)
    where oppgave.status = 'AvventerSaksbehandler'
), oid_for_tildeling as (
    select utbetaling_id, saksbehandler_ref
    from oppgave o
             join tildeling t2 on o.id = t2.oppgave_id_ref
             inner join opprinnelig_oppgave using (id)
), insert_data as (
    select saksbehandler_ref, o.id, true
    from oid_for_tildeling
             inner join oppgaveid_som_skal_tildeles o on o.utbetaling_id = oid_for_tildeling.utbetaling_id
)
insert into tildeling (saksbehandler_ref, oppgave_id_ref, på_vent) select * from insert_data;
