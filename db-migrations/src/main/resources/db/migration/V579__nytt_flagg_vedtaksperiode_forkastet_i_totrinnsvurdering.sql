alter table totrinnsvurdering add column vedtaksperiode_forkastet boolean default false;

update totrinnsvurdering tv
set vedtaksperiode_forkastet = true
from vedtak v
where tv.vedtaksperiode_id = v.vedtaksperiode_id
  and v.forkastet = true