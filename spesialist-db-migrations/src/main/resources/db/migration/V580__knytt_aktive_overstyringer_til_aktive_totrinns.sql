with aktiveOverstyringerPåAktivTotrinns as (select tv.id as totrinns_ref, o.id as overstyring_id
              from overstyring o
                       inner join overstyringer_for_vedtaksperioder ofv on ofv.overstyring_ref = o.id
                       inner join totrinnsvurdering tv on tv.vedtaksperiode_id = ofv.vedtaksperiode_id
                       inner join vedtak v on tv.vedtaksperiode_id = v.vedtaksperiode_id
              where tv.tilstand != 'GODKJENT'
                and o.ferdigstilt = false
                and v.forkastet = false)
update overstyring o
set totrinnsvurdering_ref = cte.totrinns_ref
from aktiveOverstyringerPåAktivTotrinns cte
where cte.overstyring_id = o.id and o.totrinnsvurdering_ref is null;