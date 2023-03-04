alter table reserver_person alter column gyldig_til set default now() + interval '72 hours';

update reserver_person set gyldig_til = '2023-03-06 17:00:00.000000'
where gyldig_til > now();
