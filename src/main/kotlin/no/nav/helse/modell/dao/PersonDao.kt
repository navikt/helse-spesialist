package no.nav.helse.modell.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import javax.sql.DataSource

class PersonDao(private val dataSource: DataSource) {
    fun finnPerson(fødselsnummer: Long): Boolean = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT id FROM person WHERE fodselsnummer=?;", fødselsnummer)
                .map { true }
                .asSingle
        ) == true
    }

    fun insertNavn(fornavn: String, mellomnavn: String?, etternavn: String): Int =
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

    fun insertPerson(fødselsnummer: Long, aktørId: Long, navnId: Int, enhetId: Int) =
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

    fun setNavn(fødselsnummer: Long, fornavn: String, mellomnavn: String?, etternavn: String) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "UPDATE person_navn SET fornavn=?, mellomnavn=?, etternavn=?, oppdatert=now() WHERE id=(SELECT navn_ref FROM person WHERE fodselsnummer=?);",
                    fornavn, mellomnavn, etternavn, fødselsnummer
                ).asUpdate
            )
        }

    fun oppdaterEnhet(fødselsnummer: Long, enhetNr: Int) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "UPDATE person SET enhet_ref=?, enhet_ref_oppdatert=now() WHERE fodselsnummer=?;",
                enhetNr,
                fødselsnummer
            ).asUpdate
        )
    }

    fun navnSistOppdatert(fødselsnummer: Long) = requireNotNull(using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "SELECT oppdatert FROM person_navn WHERE id=(SELECT navn_ref FROM person WHERE fodselsnummer=?);",
                fødselsnummer
            ).map {
                it.sqlDate("oppdatert").toLocalDate()
            }.asSingle
        )
    })

    fun enhetSistOppdatert(fødselsnummer: Long) = requireNotNull(using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "SELECT enhet_ref_oppdatert FROM person WHERE fodselsnummer=?;",
                fødselsnummer
            ).map {
                it.sqlDate("enhet_ref_oppdatert").toLocalDate()
            }.asSingle
        )
    })
}
