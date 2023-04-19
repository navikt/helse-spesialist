ALTER TABLE selve_vedtaksperiode_generasjon ADD COLUMN tilstand VARCHAR;
UPDATE selve_vedtaksperiode_generasjon SET tilstand = CASE låst WHEN true THEN 'Låst' ELSE 'Ulåst' END;
ALTER TABLE selve_vedtaksperiode_generasjon ALTER COLUMN tilstand SET NOT NULL;