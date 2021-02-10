select id into lol FROM arbeidsgiver
WHERE id NOT IN (
    SELECT min(id)
    FROM   arbeidsgiver
    GROUP  BY orgnummer);

delete from arbeidsgiver_navn
where id in (select navn_ref from arbeidsgiver a join lol on a.id = lol.id);

delete from arbeidsgiver_bransjer
where id in (select bransjer_ref from arbeidsgiver a join lol on a.id = lol.id);

delete from arbeidsgiver
where id in (select * from lol);
drop table if exists lol;

ALTER TABLE arbeidsgiver ADD UNIQUE(orgnummer);
