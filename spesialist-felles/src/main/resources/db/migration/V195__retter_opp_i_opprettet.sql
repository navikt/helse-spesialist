/*

Alle ca 20 000 oppgaver åpne oppgave ble opprettet på nytt pga en feilfiks, og fikk
derfor nytt klokkkeslett i opprettet-kolonnen, som er den saksbehandler sorterer
oppgavelista etter for å få tatt eldste saker først.

Oppdatert-kolonnen forblir urørt og vil vise datoen for nyopprettelsen ifm feilfiksen,
i hvert fall fram til noe annet oppdaterer den :D

*/

with oppslag as (select vedtak_ref, max(opprettet) as høyeste_per_vedtak
                 from oppgave
                 where status <> 'AvventerSaksbehandler'
                 group by vedtak_ref)
update oppgave o
set opprettet = coalesce(oppslag.høyeste_per_vedtak, o.opprettet)
from oppslag
where oppslag.vedtak_ref = o.vedtak_ref
  and o.status = 'AvventerSaksbehandler'
