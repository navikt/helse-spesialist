create table utbetalingslinje
(
    id             SERIAL PRIMARY KEY,
    oppdrag_id     bigint               not null references oppdrag (id),
    delytelseId    int                  not null,
    refDelytelseId int,
    refFagsystemId varchar(32),
    endringskode   oppdrag_endringskode not null,
    klassekode     oppdrag_klassekode   not null,
    statuskode     oppdrag_statuskode,
    datoStatusFom  date,
    fom            date                 not null,
    tom            date                 not null,
    dagsats        int                  not null,
    lønn           int                  not null,
    grad           int                  not null
);


create TYPE utbetalingslinje_type AS
(
    fom              date,
    tom              date,
    dagsats          int,
    lønn             int,
    grad             decimal,
    "refFagsystemId" int,
    "delytelseId"    int,
    "datoStatusFom"  date,
    statuskode       oppdrag_statuskode,
    "refDelytelseId" int,
    endringskode     oppdrag_endringskode,
    klassekode       oppdrag_klassekode

);
