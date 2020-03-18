package no.nav.helse.modell.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.time.LocalDate
import javax.sql.DataSource

class ArbeidsgiverDao(private val dataSource: DataSource) {
    fun finnArbeidsgiver(orgnummer: Long): Int? = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT id FROM arbeidsgiver WHERE orgnummer=?;", orgnummer)
                .map { it.int("id") }
                .asSingle
        )
    }

    fun opprettArbeidsgiver(orgnummer: Long, navn: String) =
        using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            val navnRef = requireNotNull(
                session.run(
                    queryOf("INSERT INTO arbeidsgiver_navn(navn) VALUES(?);", navn)
                        .asUpdateAndReturnGeneratedKey
                )
            )
            session.run(
                queryOf("INSERT INTO arbeidsgiver(orgnummer, navn_ref) VALUES(?, ?);", orgnummer, navnRef)
                    .asUpdate
            )
        }

    fun navnSistOppdatert(orgnummer: Long): LocalDate = requireNotNull(using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "SELECT navn_oppdatert FROM arbeidsgiver_navn WHERE id=(SELECT navn_ref FROM arbeidsgiver WHERE orgnummer=?);",
                orgnummer
            ).map {
                it.localDate("navn_oppdatert")
            }.asSingle
        )
    })

    fun oppdaterNavn(orgnummer: String, navn: String) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "UPDATE arbeidsgiver_navn SET navn=? WHERE id(SELECT navn_ref FROM arbeidsgiver WHERE orgnummer=?);",
                navn,
                orgnummer
            ).asUpdate
        )
    }
}
