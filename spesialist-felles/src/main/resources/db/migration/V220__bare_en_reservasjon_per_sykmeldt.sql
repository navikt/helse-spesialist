-- Fjern utdaterte rader som peker på samme vedtak som en nyere rad:
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
