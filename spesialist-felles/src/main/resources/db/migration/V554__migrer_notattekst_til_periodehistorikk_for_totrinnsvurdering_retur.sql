update periodehistorikk ph
set json = jsonb_set(json::jsonb, '{notattekst}', to_jsonb(n.tekst), true)
from notat n
where ph.dialog_ref = n.dialog_ref
  and ph.type = 'TOTRINNSVURDERING_RETUR';
