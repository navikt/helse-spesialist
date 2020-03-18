package no.nav.helse.modell.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.løsning.PersonEgenskap
import javax.sql.DataSource

class PersonDao(private val dataSource: DataSource) {
    internal fun finnPerson(fødselsnummer: Long): Boolean = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT id FROM person WHERE fodselsnummer=?;", fødselsnummer)
                .map { true }
                .asSingle
        ) == true
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

    internal fun setNavn(fødselsnummer: Long, fornavn: String, mellomnavn: String?, etternavn: String) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "UPDATE person_navn SET fornavn=?, mellomnavn=?, etternavn=?, oppdatert=now() WHERE id=(SELECT navn_ref FROM person WHERE fodselsnummer=?);",
                    fornavn, mellomnavn, etternavn, fødselsnummer
                ).asUpdate
            )
        }

    internal fun oppdaterEnhet(fødselsnummer: Long, enhetNr: Int) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "UPDATE person SET enhet_ref=?, enhet_ref_oppdatert=now() WHERE fodselsnummer=?;",
                enhetNr,
                fødselsnummer
            ).asUpdate
        )
    }

    internal fun navnSistOppdatert(fødselsnummer: Long) = requireNotNull(using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "SELECT oppdatert FROM person_navn WHERE id=(SELECT navn_ref FROM person WHERE fodselsnummer=?);",
                fødselsnummer
            ).map {
                it.sqlDate("oppdatert").toLocalDate()
            }.asSingle
        )
    })

    internal fun enhetSistOppdatert(fødselsnummer: Long) = requireNotNull(using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "SELECT enhet_ref_oppdatert FROM person WHERE fodselsnummer=?;",
                fødselsnummer
            ).map {
                it.sqlDate("enhet_ref_oppdatert").toLocalDate()
            }.asSingle
        )
    })

    internal fun oppdaterEgenskap(fødselsnummer: Long, egenskap: PersonEgenskap?) =
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
}
