alter table vedtak add column behandlet_av_spleis boolean not null default true;
alter table vedtak alter column behandlet_av_spleis set default false;
