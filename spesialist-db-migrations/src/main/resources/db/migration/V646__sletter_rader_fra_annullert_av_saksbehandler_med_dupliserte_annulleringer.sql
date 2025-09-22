with annullering as (
    select
        id,
        vedtaksperiode_id,
        row_number() over (partition by vedtaksperiode_id order by annullert_tidspunkt) as rn
    from annullert_av_saksbehandler
)
delete from annullert_av_saksbehandler
where id in (select id from annullering where rn > 1)
