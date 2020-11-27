drop type oppdrag_type;

alter table oppdrag
    alter column mottaker set not null,
    alter column fagområde set not null,
    alter column endringskode set not null;
