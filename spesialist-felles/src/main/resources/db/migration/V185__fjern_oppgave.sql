/*
ID funnet med følgende spørring (og dobbeltsjekking mot logger/spanner):

select o.*, v.fom, v.tom, v.vedtaksperiode_id
from oppgave o
         inner join vedtak v on o.vedtak_ref = v.id
         inner join person p on v.person_ref = p.id
where p.aktor_id = <aktørId>

*/

UPDATE oppgave
SET status='Invalidert'::oppgavestatus
WHERE id = 2651683;
