delete from kommentarer;

alter table kommentarer
    add column opprettet timestamp not null default now(),
    add column saksbehandlerident varchar(64) not null;