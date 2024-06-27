UPDATE overstyring o SET vedtaksperiode_id=ofv.vedtaksperiode_id
     FROM overstyringer_for_vedtaksperioder ofv
     where o.id=ofv.overstyring_ref
       AND o.vedtaksperiode_id='00000000-0000-0000-aaaa-bbbbbbbbbbbb'
