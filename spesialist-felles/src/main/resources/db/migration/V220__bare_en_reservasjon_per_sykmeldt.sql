-- Må først fjerne duplikate rader i dev, de tas ikke av neste spørringπ
DELETE
FROM reserver_person a
    USING
        (SELECT max(ctid) as ctid, person_ref
         FROM reserver_person
         GROUP BY person_ref, gyldig_til
         HAVING COUNT(*) > 1) b
WHERE a.person_ref = b.person_ref
  AND a.ctid <> b.ctid;

-- Fjern eldre reservasjonsrader enn nyeste
with slettes as
         (select resper.gyldig_til,
                 resper.person_ref,
                 rank() over (
                     partition by resper.person_ref
                     order by gyldig_til desc
                     ) rangering
          from reserver_person resper)
delete
from reserver_person rp
    using slettes
where rp.person_ref = slettes.person_ref
  and rp.gyldig_til = slettes.gyldig_til
  and slettes.rangering > 1;

alter table reserver_person
    add unique (person_ref);
