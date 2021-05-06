INSERT INTO vedtaksperiode_hendelse (vedtaksperiode_ref, hendelse_ref) SELECT v.id, h.id FROM hendelse h, vedtak v WHERE h.spleis_referanse = v.vedtaksperiode_id;
