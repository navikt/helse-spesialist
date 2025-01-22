alter table avviksvurdering add column slettet timestamp;

with ta_vare_paa as (
    select avviksvurdering.opprettet, vpa.vilkårsgrunnlag_id, id, row_number() over (partition by vpa.vilkårsgrunnlag_id order by avviksvurdering.opprettet desc) as rn
    from avviksvurdering
    join vilkarsgrunnlag_per_avviksvurdering vpa on avviksvurdering.unik_id = vpa.avviksvurdering_ref
)

update avviksvurdering av
set slettet = now()
from ta_vare_paa
where av.id = ta_vare_paa.id and ta_vare_paa.rn > 1;
