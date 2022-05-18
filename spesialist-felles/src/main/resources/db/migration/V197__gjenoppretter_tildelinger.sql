/*

Info om tildelte oppgaver ble borte ifm at alle oppgaver ble opprettet på nytt.

Denne spørringen finner tildelingsinfo for forrige oppgave for en vedtaksperiode
og inserter infoen - det er bare fint at den forrige tildelingen blir liggende igjen
i tabellen, i tilfelle vi må feilsøke og fikse noe mer.

*/
with ikketildelte as (select *
                      from oppgave o
                      where status = 'AvventerSaksbehandler'
                        and not exists(select 1 from tildeling t where t.oppgave_id_ref = o.id)),
     oppslag as (select t.*, o2.vedtak_ref
                 from tildeling t
                          inner join oppgave o2 on t.oppgave_id_ref = o2.id
                          inner join ikketildelte it on it.vedtak_ref = o2.vedtak_ref
                 where o2.oppdatert > '2022-05-13'
                   and o2.oppdatert < '2022-05-14')
insert
into tildeling
select oppslag.saksbehandler_ref, oppslag.gyldig_til, ikketildelte.id, oppslag.på_vent
from ikketildelte,
     oppslag
where ikketildelte.vedtak_ref = oppslag.vedtak_ref
