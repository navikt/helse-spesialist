with all_periodehistorikk_tilhørende_pa_vent as (select pv.vedtaksperiode_id, timestamp, ph.dialog_ref as dialog_ref
              from periodehistorikk ph
                       inner join selve_vedtaksperiode_generasjon svg on ph.generasjon_id = svg.unik_id
                       inner join pa_vent pv on svg.vedtaksperiode_id = pv.vedtaksperiode_id
              where type = 'LEGG_PA_VENT')
update pa_vent pv
set dialog_ref = siste_periodehistorikk_tilhørende_pa_vent.dialog_ref
from (select distinct on (vedtaksperiode_id) vedtaksperiode_id, dialog_ref
      from all_periodehistorikk_tilhørende_pa_vent
      order by vedtaksperiode_id, timestamp DESC) siste_periodehistorikk_tilhørende_pa_vent
where pv.vedtaksperiode_id = siste_periodehistorikk_tilhørende_pa_vent.vedtaksperiode_id and pv.dialog_ref is null;