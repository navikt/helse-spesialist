ALTER TABLE overstyring ADD COLUMN totrinnsvurdering_ref BIGINT REFERENCES totrinnsvurdering(id);
