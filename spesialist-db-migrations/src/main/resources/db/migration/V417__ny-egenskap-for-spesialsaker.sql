create table spesialsak (
    vedtaksperiode_id uuid not null references vedtak (vedtaksperiode_id)
)
