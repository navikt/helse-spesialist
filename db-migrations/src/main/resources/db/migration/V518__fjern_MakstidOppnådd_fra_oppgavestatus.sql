-- Fjerner MakstidOppnådd fra enum oppgavestatus
-- Postgres støtter ikke å fjerne verdier fra en enum, derfor må den opprettes på nytt

alter type oppgavestatus rename to oppgavestatus_old;
create type oppgavestatus as enum ('AvventerSystem', 'AvventerSaksbehandler', 'Ferdigstilt', 'Invalidert');

drop index oppgave_status_idx;
alter table oppgave alter column status type oppgavestatus using status::text::oppgavestatus;
alter table arkiv_oppgave_uten_utbetaling_id alter column status type oppgavestatus using status::text::oppgavestatus;

create index oppgave_status_idx on oppgave (status)
    where (status = 'AvventerSaksbehandler'::oppgavestatus);

drop type oppgavestatus_old;
