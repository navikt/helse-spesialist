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

    internal fun findPerson(id: Long): PersonDto? = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT p.fodselsnummer, n.fornavn, n.mellomnavn, n.etternavn FROM person AS p JOIN person_navn AS n ON p.navn_ref = n.id WHERE p.id=?;", id)
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

    internal fun insertNavn(fornavn: String, mellomnavn: String?, etternavn: String): Int =
        requireNotNull(using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO person_navn(fornavn, mellomnavn, etternavn) VALUES(?, ?, ?);",
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
                    "INSERT INTO person(fodselsnummer, aktor_id, navn_ref, enhet_ref) VALUES(?, ?, ?, ?);",
                    fødselsnummer,
                    aktørId,
                    navnId,
                    enhetId
                ).asExecute
            )
        }

    internal fun updateNavn(fødselsnummer: Long, fornavn: String, mellomnavn: String?, etternavn: String) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "UPDATE person_navn SET fornavn=?, mellomnavn=?, etternavn=?, oppdatert=now() WHERE id=(SELECT navn_ref FROM person WHERE fodselsnummer=?);",
                    fornavn, mellomnavn, etternavn, fødselsnummer
                ).asUpdate
            )
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

    internal fun updateEgenskap(fødselsnummer: Long, egenskap: PersonEgenskap?) =
        using(sessionOf(dataSource)) { session ->
            val id = requireNotNull(session.run(
                queryOf("SELECT id FROM person WHERE fodselsnummer=?;", fødselsnummer)
                    .map { it.int("id") }
                    .asSingle
            ))

            session.run(
                queryOf("DELETE FROM person_egenskap WHERE person_ref=?;", id)
                    .asUpdate
            )
            egenskap?.also {
                val typeRef = requireNotNull(session.run(
                    queryOf("SELECT id FROM person_egenskap_type WHERE type=?;", egenskap)
                        .map { it.int("id") }
                        .asSingle
                ))
                session.run(
                    queryOf("INSERT INTO person_egenskap(type_ref, person_ref) VALUES(?, ?);", typeRef, id)
                        .asUpdate
                )
            }

        }

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()

}
