CREATE TABLE midlertidig_behandling_vedtak_fattet (
    spleis_behandling_id UUID PRIMARY KEY REFERENCES behandling(spleis_behandling_id)
)
