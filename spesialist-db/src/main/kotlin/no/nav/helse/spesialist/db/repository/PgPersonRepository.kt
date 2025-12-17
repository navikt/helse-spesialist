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
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse
import no.nav.helse.spesialist.domain.Personinfo.Kjønn

internal class PgPersonRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    PersonRepository {
    override fun lagre(person: Person) {
        val personinfoRef =
            personinfoId(person.id)?.also {
                oppdaterPersoninfo(it, person.info)
            } ?: lagrePersoninfo(person.id, person.info)
        lagrePerson(person, personinfoRef)
        lagreEgenAnsattStatus(person.id, person.egenAnsattStatus)
    }

    private fun lagrePerson(
        person: Person,
        personinfoRef: Long?,
    ) {
        asSQL(
            """
                INSERT INTO person(fødselsnummer, aktør_id, info_ref, enhet_ref, infotrygdutbetalinger_ref, personinfo_oppdatert, enhet_ref_oppdatert, infotrygdutbetalinger_oppdatert) 
                VALUES(:fodselsnummer, :aktorId, :personinfoRef, :enhetRef, :infotrygdutbetalingerRef, :infoOppdatert, :enhetOppdatert, :infotrygdutbetalingerOppdatert)
                ON CONFLICT(fødselsnummer) DO UPDATE SET 
                info_ref = excluded.info_ref,
                personinfo_oppdatert = excluded.personinfo_oppdatert, 
                enhet_ref = excluded.enhet_ref,
                enhet_ref_oppdatert = excluded.enhet_ref_oppdatert,
                infotrygdutbetalinger_ref = excluded.infotrygdutbetalinger_ref,
                infotrygdutbetalinger_oppdatert = excluded.infotrygdutbetalinger_oppdatert
            """,
            "fodselsnummer" to person.id.value,
            "aktorId" to person.aktørId,
            "personinfoRef" to personinfoRef,
            "enhetRef" to person.enhetRef,
            "infotrygdutbetalingerRef" to person.infotrygdutbetalingerRef,
            "infoOppdatert" to person.infoOppdatert,
            "enhetOppdatert" to person.enhetRefOppdatert,
            "infotrygdutbetalingerOppdatert" to person.infotrygdutbetalingerOppdatert,
        ).update()
    }

    private fun lagrePersoninfo(
        identitetsnummer: Identitetsnummer,
        personinfo: Personinfo?,
    ): Long? {
        if (personinfo == null) return null
        return asSQL(
            """
                     INSERT INTO person_info(fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse) 
                     VALUES(:fornavn, :mellomnavn, :etternavn, :fodselsdato, :kjonn::person_kjonn, :adressebeskyttelse)
                """,
            "fornavn" to personinfo.fornavn,
            "mellomnavn" to personinfo.mellomnavn,
            "etternavn" to personinfo.etternavn,
            "fodselsdato" to personinfo.fødselsdato,
            "kjonn" to
                personinfo.kjønn?.let {
                    when (it) {
                        Kjønn.Kvinne -> "Kvinne"
                        Kjønn.Mann -> "Mann"
                        Kjønn.Ukjent -> "Ukjent"
                    }
                },
            "adressebeskyttelse" to
                when (personinfo.adressebeskyttelse) {
                    Adressebeskyttelse.Ugradert -> "Ugradert"
                    Adressebeskyttelse.Fortrolig -> "Fortrolig"
                    Adressebeskyttelse.StrengtFortrolig -> "StrengtFortrolig"
                    Adressebeskyttelse.StrengtFortroligUtland -> "StrengtFortroligUtland"
                    Adressebeskyttelse.Ukjent -> "Ukjent"
                },
            "fodselsnummer" to identitetsnummer.value,
        ).updateAndReturnGeneratedKey()
    }

    private fun oppdaterPersoninfo(
        personinfoId: Long,
        personinfo: Personinfo?,
    ) {
        if (personinfo == null) return
        asSQL(
            """
                     UPDATE person_info SET fornavn = :fornavn, mellomnavn = :mellomnavn, etternavn = :etternavn,
                     fodselsdato = :fodselsdato, kjonn = :kjonn::person_kjonn, adressebeskyttelse = :adressebeskyttelse
                     WHERE id = :personinfoRef
                """,
            "fornavn" to personinfo.fornavn,
            "mellomnavn" to personinfo.mellomnavn,
            "etternavn" to personinfo.etternavn,
            "fodselsdato" to personinfo.fødselsdato,
            "kjonn" to
                personinfo.kjønn?.let {
                    when (it) {
                        Kjønn.Kvinne -> "Kvinne"
                        Kjønn.Mann -> "Mann"
                        Kjønn.Ukjent -> "Ukjent"
                    }
                },
            "adressebeskyttelse" to
                when (personinfo.adressebeskyttelse) {
                    Adressebeskyttelse.Ugradert -> "Ugradert"
                    Adressebeskyttelse.Fortrolig -> "Fortrolig"
                    Adressebeskyttelse.StrengtFortrolig -> "StrengtFortrolig"
                    Adressebeskyttelse.StrengtFortroligUtland -> "StrengtFortroligUtland"
                    Adressebeskyttelse.Ukjent -> "Ukjent"
                },
            "personinfoRef" to personinfoId,
        ).update()
    }

    private fun personinfoId(identitetsnummer: Identitetsnummer): Long? =
        asSQL(
            """
               SELECT info_ref FROM person WHERE fødselsnummer = :fodselsnummer 
            """,
            "fodselsnummer" to identitetsnummer.value,
        ).singleOrNull { row -> row.longOrNull("info_ref") }

    private fun lagreEgenAnsattStatus(
        identitetsnummer: Identitetsnummer,
        egenAnsattStatus: EgenAnsattStatus?,
    ) {
        if (egenAnsattStatus == null) return
        asSQL(
            """
                 INSERT INTO egen_ansatt(person_ref, er_egen_ansatt, opprettet) 
                  VALUES ((SELECT id FROM person WHERE fødselsnummer = :fodselsnummer), :egenAnsattStatus, :egenAnsattStatusOppdatert)
                  ON CONFLICT(person_ref) DO UPDATE set er_egen_ansatt = excluded.er_egen_ansatt, opprettet = excluded.opprettet
            """,
            "egenAnsattStatus" to egenAnsattStatus.erEgenAnsatt,
            "egenAnsattStatusOppdatert" to egenAnsattStatus.oppdatertTidspunkt,
            "fodselsnummer" to identitetsnummer.value,
        ).update()
    }

    override fun finn(id: Identitetsnummer): Person? =
        asSQL(
            """
            SELECT p.*, pi.id as person_info_id, pi.*, ea.person_ref as egen_ansatt_person_ref, ea.er_egen_ansatt, ea.opprettet as egen_ansatt_opprettet
            FROM person p
            LEFT JOIN person_info pi ON p.info_ref = pi.id
            LEFT JOIN egen_ansatt ea ON p.id = ea.person_ref
            WHERE p.fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to id.value,
        ).singleOrNull { it.toPerson() }

    override fun finnAlle(ider: Set<Identitetsnummer>): List<Person> =
        asSQL(
            """
            SELECT p.*, pi.id as person_info_id, pi.*, ea.person_ref as egen_ansatt_person_ref, ea.er_egen_ansatt, ea.opprettet as egen_ansatt_opprettet
            FROM person p
            LEFT JOIN person_info pi ON p.info_ref = pi.id
            LEFT JOIN egen_ansatt ea ON p.id = ea.person_ref
            WHERE p.fødselsnummer = ANY (:fodselsnumre)
            """.trimIndent(),
            "fodselsnumre" to ider.map { it.value }.toTypedArray(),
        ).list { it.toPerson() }

    private fun Row.toPerson(): Person =
        Person.Factory.fraLagring(
            id = Identitetsnummer.fraString(string("fødselsnummer")),
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
            infoOppdatert = localDateOrNull("personinfo_oppdatert"),
            enhetRef = intOrNull("enhet_ref"),
            enhetRefOppdatert = localDateOrNull("enhet_ref_oppdatert"),
            infotrygdutbetalingerRef = intOrNull("infotrygdutbetalinger_ref"),
            infotrygdutbetalingerOppdatert = localDateOrNull("infotrygdutbetalinger_oppdatert"),
            egenAnsattStatus =
                longOrNull("egen_ansatt_person_ref")?.let {
                    EgenAnsattStatus(
                        erEgenAnsatt = boolean("er_egen_ansatt"),
                        oppdatertTidspunkt = instant("egen_ansatt_opprettet"),
                    )
                },
        )
}
