DELETE FROM periodehistorikk ph USING (
    SELECT MIN(id) as id, notat_id
    FROM periodehistorikk
    GROUP BY notat_id HAVING COUNT(*) > 1
) b
WHERE ph.notat_id = b.notat_id
  AND ph.id <> b.id;
