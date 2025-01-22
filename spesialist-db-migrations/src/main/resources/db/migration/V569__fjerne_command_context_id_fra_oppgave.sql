with manglende as (
    select command_context_id, id from oppgave
    where hendelse_id_godkjenningsbehov is null
)
update oppgave
set hendelse_id_godkjenningsbehov = h.id
from manglende
join command_context cc on manglende.command_context_id = cc.context_id
join hendelse h on cc.hendelse_id = h.id
where oppgave.id = manglende.id;

alter table oppgave
    alter column hendelse_id_godkjenningsbehov set not null;

alter table oppgave
    drop column command_context_id;
