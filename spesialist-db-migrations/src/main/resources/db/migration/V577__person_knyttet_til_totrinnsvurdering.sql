alter table totrinnsvurdering
    add column person_ref bigint references person (id);

update totrinnsvurdering tv
set person_ref = v.person_ref
from vedtak v
where tv.vedtaksperiode_id = v.vedtaksperiode_id;

alter table totrinnsvurdering
    alter column person_ref set not null;