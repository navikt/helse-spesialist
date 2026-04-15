create table venter_på_kvittering_for_overstyring (
    melding_id uuid not null,
    identitetsnummer varchar not null,
    primary key (melding_id)
)
