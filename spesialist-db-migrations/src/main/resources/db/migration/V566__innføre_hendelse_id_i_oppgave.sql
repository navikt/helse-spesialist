create temp table oppslag as
select hendelse_id, o.id as oppgave_id
from oppgave o,
     lateral (select cc.hendelse_id
              from command_context cc
              where o.command_context_id = cc.context_id
              limit 1) hendelse_og_oppgave;

alter table oppgave
    add column hendelse_id_godkjenningsbehov uuid;

update oppgave o
set hendelse_id_godkjenningsbehov = oppslag.hendelse_id
from oppslag
where oppslag.oppgave_id = o.id;
