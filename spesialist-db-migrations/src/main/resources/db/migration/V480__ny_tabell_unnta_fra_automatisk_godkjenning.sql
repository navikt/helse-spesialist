create table unnta_fra_automatisk_godkjenning
(
    fødselsnummer bigint primary key
        references person (fodselsnummer),
    unnta         boolean,
    årsaker       varchar[] not null default array []::varchar[],
    oppdatert     timestamp          default now()
)
