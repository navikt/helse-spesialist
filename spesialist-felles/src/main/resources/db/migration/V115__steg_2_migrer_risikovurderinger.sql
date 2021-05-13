INSERT INTO risikovurdering_2021 (vedtaksperiode_id, kan_godkjennes_automatisk, krever_supersaksbehandler, data, opprettet)
SELECT
    r.vedtaksperiode_id,
    COUNT (ra.tekst) = 0 kan_godkjennes_automatisk,
    FALSE krever_supersaksbehandler,
    CAST ('{
      "@opprettet": "' || r.opprettet || '",
      "vedtaksperiodeId": "' || r.vedtaksperiode_id || '",
      "ufullstendig": ' || r.ufullstendig || ',
      "funn": [
        ' || CASE WHEN COUNT (ra.tekst) = 0 THEN '' ELSE string_agg('{
          "kreverSupersaksbehandler": false,
          "beskrivelse": "' || ra.tekst || '",
          "kategori": [ "8-4" ]
        }', ',' ) END || '
      ],
      "kontrollertOk": [],
      "kanGodkjennesAutomatisk": ' || (COUNT (ra.tekst) = 0)::text || '
}' AS jsonb) AS data,
    r.opprettet
FROM risikovurdering r
LEFT JOIN risikovurdering_arbeidsuforhetvurdering ra ON r.id = ra.risikovurdering_ref
WHERE r.opprettet >= '2021-01-25 00:00:00.000000'
GROUP BY r.id
ORDER BY r.id;
