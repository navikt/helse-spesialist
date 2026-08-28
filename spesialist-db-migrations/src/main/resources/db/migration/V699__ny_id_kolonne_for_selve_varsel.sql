ALTER TABLE selve_varsel ADD COLUMN id UUID UNIQUE DEFAULT gen_random_uuid();
