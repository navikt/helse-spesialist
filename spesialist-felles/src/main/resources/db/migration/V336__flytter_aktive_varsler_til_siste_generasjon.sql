UPDATE selve_varsel SET generasjon_ref = '6552362' WHERE generasjon_ref = (SELECT id from selve_vedtaksperiode_generasjon WHERE unik_id = '0841d430-7ad0-4a0f-a16e-403b6a9bed90') AND status = 'AKTIV' AND kode NOT IN (SELECT kode FROM selve_varsel WHERE generasjon_ref = '6552362');
UPDATE selve_varsel SET generasjon_ref = '6598005' WHERE generasjon_ref = (SELECT id from selve_vedtaksperiode_generasjon WHERE unik_id = '113bfce1-6d74-4b1f-99be-967a10cd0733') AND status = 'AKTIV' AND kode NOT IN (SELECT kode FROM selve_varsel WHERE generasjon_ref = '6598005');
UPDATE selve_varsel SET generasjon_ref = '6614798' WHERE generasjon_ref = (SELECT id from selve_vedtaksperiode_generasjon WHERE unik_id = '28f06745-dc40-4c16-94d1-c9754d13f698') AND status = 'AKTIV' AND kode NOT IN (SELECT kode FROM selve_varsel WHERE generasjon_ref = '6614798');
UPDATE selve_varsel SET generasjon_ref = '6609124' WHERE generasjon_ref = (SELECT id from selve_vedtaksperiode_generasjon WHERE unik_id = '36cd9d8b-d7f1-4df7-9810-1d4dca8663bf') AND status = 'AKTIV' AND kode NOT IN (SELECT kode FROM selve_varsel WHERE generasjon_ref = '6609124');
UPDATE selve_varsel SET generasjon_ref = '6542559' WHERE generasjon_ref = (SELECT id from selve_vedtaksperiode_generasjon WHERE unik_id = '5ca5cc04-1906-402d-bf40-26e664d01792') AND status = 'AKTIV' AND kode NOT IN (SELECT kode FROM selve_varsel WHERE generasjon_ref = '6542559');
UPDATE selve_varsel SET generasjon_ref = '6549465' WHERE generasjon_ref = (SELECT id from selve_vedtaksperiode_generasjon WHERE unik_id = '5d3ad0fb-8627-4e17-a47c-4c1830b08745') AND status = 'AKTIV' AND kode NOT IN (SELECT kode FROM selve_varsel WHERE generasjon_ref = '6549465');
UPDATE selve_varsel SET generasjon_ref = '6609974' WHERE generasjon_ref = (SELECT id from selve_vedtaksperiode_generasjon WHERE unik_id = '736a8673-7368-44c5-b6e4-99c2910181d9') AND status = 'AKTIV' AND kode NOT IN (SELECT kode FROM selve_varsel WHERE generasjon_ref = '6609974');
UPDATE selve_varsel SET generasjon_ref = '6614800' WHERE generasjon_ref = (SELECT id from selve_vedtaksperiode_generasjon WHERE unik_id = 'ba8d3838-a899-40ce-b851-f3a8a7f8e3a1') AND status = 'AKTIV' AND kode NOT IN (SELECT kode FROM selve_varsel WHERE generasjon_ref = '6614800');