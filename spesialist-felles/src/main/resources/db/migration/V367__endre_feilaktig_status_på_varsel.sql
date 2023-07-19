UPDATE selve_varsel
SET status = 'AKTIV', status_endret_tidspunkt = null, status_endret_ident = null
WHERE unik_id IN (
    'd4ed9abd-d351-4683-861d-84a6c36dfc2a',
    'cfcc59d7-2329-48aa-87fc-ff3b04d609c6',
    '51a581b9-840b-4f83-a28c-7363c8c4dbe1'
);