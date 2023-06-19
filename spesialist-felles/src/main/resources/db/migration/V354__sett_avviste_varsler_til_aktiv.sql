-- Setter status aktiv på to avviste varsler som blokkerte for videre saksbehandling. Redteam-sak fra 16/06/23
UPDATE selve_varsel SET status = 'AKTIV' WHERE id in (1354211, 1354225);