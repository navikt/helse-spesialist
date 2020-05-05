package no.nav.helse.modell.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.dto.NavnDto
import no.nav.helse.modell.dto.PersonDto
import no.nav.helse.modell.løsning.PersonEgenskap
import javax.sql.DataSource

class PersonDao(private val dataSource: DataSource) {
    internal fun findPersonByFødselsnummer(fødselsnummer: Long): Int? = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT id FROM person WHERE fodselsnummer=?;", fødselsnummer)
                .map { it.int("id") }
                .asSingle
        )
    }

    internal fun findPersonByAktørId(aktørId: Long): Int? = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT id FROM person WHERE aktor_id=?;", aktørId)
                .map { it.int("id") }
                .asSingle
        )
    }

    internal fun findPerson(id: Long): PersonDto? = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT p.fodselsnummer, pi.fornavn, pi.mellomnavn, pi.etternavn FROM person AS p JOIN person_info AS pi ON p.info_ref = pi.id WHERE p.id=?;", id)
                .map { PersonDto(
                    fødselsnummer = it.long("fodselsnummer").toFødselsnummer(),
                    navn = NavnDto(
                        fornavn = it.string("fornavn"),
                        mellomnavn = it.stringOrNull("mellomnavn"),
                        etternavn = it.string("etternavn")
                    )
                ) }
                .asSingle
        )
    }

    internal fun findNavn(personId: Int): NavnDto? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT * from person_info WHERE id=(SELECT info_ref FROM person where id =?);",
                    personId
                ).map { NavnDto(it.string("fornavn"), it.stringOrNull("mellomnavn"), it.string("etternavn")) }.asSingle
            )
        }


    internal fun insertNavn(fornavn: String, mellomnavn: String?, etternavn: String): Int =
        requireNotNull(using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO person_info(fornavn, mellomnavn, etternavn) VALUES(?, ?, ?);",
                    fornavn,
                    mellomnavn,
                    etternavn
                ).asUpdateAndReturnGeneratedKey
            )
        }?.toInt())

    internal fun insertPerson(fødselsnummer: Long, aktørId: Long, navnId: Int, enhetId: Int) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO person(fodselsnummer, aktor_id, info_ref, enhet_ref) VALUES(?, ?, ?, ?);",
                    fødselsnummer,
                    aktørId,
                    navnId,
                    enhetId
                ).asExecute
            )
        }

    internal fun updateNavn(fødselsnummer: Long, fornavn: String, mellomnavn: String?, etternavn: String) =
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        "UPDATE person_info SET fornavn=?, mellomnavn=?, etternavn=? WHERE id=(SELECT info_ref FROM person WHERE fodselsnummer=?);",
                        fornavn, mellomnavn, etternavn, fødselsnummer
                    ).asUpdate
                )
                tx.run(queryOf("UPDATE person SET personinfo_oppdatert=now() WHERE fodselsnummer=?;", fødselsnummer).asUpdate) }
        }

    internal fun updateEnhet(fødselsnummer: Long, enhetNr: Int) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "UPDATE person SET enhet_ref=?, enhet_ref_oppdatert=now() WHERE fodselsnummer=?;",
                enhetNr,
                fødselsnummer
            ).asUpdate
        )
    }

    internal fun findPersoninfoSistOppdatert(fødselsnummer: Long) =
        requireNotNull(using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT personinfo_oppdatert FROM person WHERE fodselsnummer=?;",
                    fødselsnummer
                ).map {
                    it.sqlDate("personinfo_oppdatert").toLocalDate()
                }.asSingle
            )
        })

    internal fun findEnhetSistOppdatert(fødselsnummer: Long) = requireNotNull(using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "SELECT enhet_ref_oppdatert FROM person WHERE fodselsnummer=?;",
                fødselsnummer
            ).map {
                it.sqlDate("enhet_ref_oppdatert").toLocalDate()
            }.asSingle
        )
    })

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()

}
