create table annullert_av_saksbehandler(
    id                   serial          not null constraint annullert_av_saksbehnalder_pkey primary key,
    annullert_tidspunkt  date           not null,
    saksbehandler_ident  varchar(32)    not null,
    saksbehandler_epost  varchar(128)   not null,
);

alter table utbetaling_id add constraint
