create table saksbehandler_opptegnelse_sekvensnummer
(
    saksbehandler_id    uuid primary key not null references saksbehandler,
    siste_sekvensnummer int              not null
);

insert into saksbehandler_opptegnelse_sekvensnummer
select saksbehandler_id, max(siste_sekvensnummer)
from abonnement_for_opptegnelse
group by saksbehandler_id;

alter table abonnement_for_opptegnelse
    drop column siste_sekvensnummer;
