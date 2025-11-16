package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.spesialist.application.PersonRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.EgenAnsattStatus
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.PersonId
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse
import no.nav.helse.spesialist.domain.Personinfo.Kjønn

internal class PgPersonRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    PersonRepository {
    override fun finn(id: PersonId): Person? =
        asSQL(
            """
            SELECT p.*, pi.id as person_info_id, pi.*, ea.person_ref as egen_ansatt_person_ref, ea.er_egen_ansatt, ea.opprettet as egen_ansatt_opprettet
            FROM person p
            LEFT JOIN person_info pi ON p.info_ref = pi.id
            LEFT JOIN egen_ansatt ea ON p.id = ea.person_ref
            WHERE p.id = :id
            """.trimIndent(),
            "id" to id.value,
        ).singleOrNull { it.toPerson() }

    override fun finn(identitetsnummer: Identitetsnummer): Person? =
        asSQL(
            """
            SELECT p.*, pi.id as person_info_id, pi.*, ea.person_ref as egen_ansatt_person_ref, ea.er_egen_ansatt, ea.opprettet as egen_ansatt_opprettet
            FROM person p
            LEFT JOIN person_info pi ON p.info_ref = pi.id
            LEFT JOIN egen_ansatt ea ON p.id = ea.person_ref
            WHERE p.fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to identitetsnummer.value,
        ).singleOrNull { it.toPerson() }

    override fun finnAlle(ider: Set<PersonId>): List<Person> =
        asSQL(
            """
            SELECT p.*, pi.id as person_info_id, pi.*, ea.person_ref as egen_ansatt_person_ref, ea.er_egen_ansatt, ea.opprettet as egen_ansatt_opprettet
            FROM person p
            LEFT JOIN person_info pi ON p.info_ref = pi.id
            LEFT JOIN egen_ansatt ea ON p.id = ea.person_ref
            WHERE p.id = ANY (:ider)
            """.trimIndent(),
            "ider" to ider.map { it.value }.toTypedArray(),
        ).list { it.toPerson() }

    private fun Row.toPerson(): Person =
        Person.Factory.fraLagring(
            id = PersonId(int("id")),
            identitetsnummer = Identitetsnummer.fraString(string("fødselsnummer")),
            aktørId = string("aktør_id"),
            info =
                intOrNull("person_info_id")?.let {
                    Personinfo(
                        fornavn = string("fornavn"),
                        mellomnavn = stringOrNull("mellomnavn"),
                        etternavn = string("etternavn"),
                        fødselsdato = localDateOrNull("fodselsdato"),
                        kjønn = stringOrNull("kjonn")?.let(Kjønn::valueOf) ?: Kjønn.Ukjent,
                        adressebeskyttelse = enumValueOf<Adressebeskyttelse>(string("adressebeskyttelse")),
                    )
                },
            infoOppdatert = localDate("personinfo_oppdatert"),
            enhetRef = int("enhet_ref"),
            enhetRefOppdatert = localDate("enhet_ref_oppdatert"),
            infotrygdutbetalingerRef = int("infotrygdutbetalinger_ref"),
            infotrygdutbetalingerOppdatert = localDate("infotrygdutbetalinger_oppdatert"),
            egenAnsattStatus =
                longOrNull("egen_ansatt_person_ref")?.let {
                    EgenAnsattStatus.fraLagring(
                        erEgenAnsatt = boolean("er_egen_ansatt"),
                        oppdatertTidspunkt = instant("egen_ansatt_opprettet"),
                    )
                },
        )
}
