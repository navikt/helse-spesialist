UPDATE automatisering_problem ap SET utbetaling_id = a.utbetaling_id
FROM automatisering a WHERE
    a.hendelse_ref = ap.hendelse_ref AND a.vedtaksperiode_ref = ap.vedtaksperiode_ref;
