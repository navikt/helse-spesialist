/*ALTER TABLE utbetaling_id
    ADD COLUMN person_ref                    INT REFERENCES person (id),
    ADD COLUMN arbeidsgiver_ref              INT REFERENCES arbeidsgiver (id),
    ADD COLUMN arbeidsgiver_fagsystem_id_ref BIGINT REFERENCES oppdrag (id),
    ADD COLUMN person_fagsystem_id_ref       BIGINT REFERENCES oppdrag (id),
    ADD COLUMN type                          utbetaling_type,
    ADD COLUMN opprettet                     TIMESTAMP NULL
;

update utbetaling_id ui
set person_ref                    = u.person_ref,
    arbeidsgiver_ref              = u.arbeidsgiver_ref,
    arbeidsgiver_fagsystem_id_ref = u.arbeidsgiver_fagsystem_id_ref,
    person_fagsystem_id_ref       = u.person_fagsystem_id_ref,
    type                          = u.type,
    opprettet                     = u.opprettet
from utbetaling u
where u.utbetaling_id_ref = ui.id
;

alter table utbetaling
    drop column person_ref,
    drop column arbeidsgiver_ref,
    drop column arbeidsgiver_fagsystem_id_ref,
    drop column person_fagsystem_id_ref,
    drop column type
;

alter table utbetaling_id
    alter column person_ref set not null,
    alter column arbeidsgiver_ref set not null,
    alter column arbeidsgiver_fagsystem_id_ref set not null,
    alter column person_fagsystem_id_ref set not null,
    alter column type set not null,
    alter column opprettet set not null
;
*/
