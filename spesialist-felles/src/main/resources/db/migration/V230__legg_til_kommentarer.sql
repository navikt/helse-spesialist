create table kommentarer (
    id serial not null constraint kommentarer_pkey primary key,
    tekst text not null,
    notat_ref int references notat(id),
    feilregistrert_tidspunkt timestamp
);
