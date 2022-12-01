DELETE FROM warning
WHERE melding = 'Saken må revurderes fordi det har blitt behandlet en tidligere periode som kan ha betydning.'
AND opprettet >= '2022-11-20 00:00:00'::timestamp;