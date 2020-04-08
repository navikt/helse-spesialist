package no.nav.helse.modell.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.dto.ArbeidsgiverDto
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

class ArbeidsgiverDao(private val dataSource: DataSource) {
    fun findArbeidsgiverByOrgnummer(orgnummer: Long): Int? = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT id FROM arbeidsgiver WHERE orgnummer=?;", orgnummer)
                .map { it.int("id") }
                .asSingle
        )
    }

    fun insertArbeidsgiver(orgnummer: Long, navn: String) =
        using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            val navnRef = requireNotNull(
                session.run(
                    queryOf(
                        "INSERT INTO arbeidsgiver_navn(navn, navn_oppdatert) VALUES(?, ?);",
                        navn,
                        LocalDateTime.now().minusYears(1)
                    )
                        .asUpdateAndReturnGeneratedKey
                )
            )
            session.run(
                queryOf("INSERT INTO arbeidsgiver(orgnummer, navn_ref) VALUES(?, ?);", orgnummer, navnRef)
                    .asUpdate
            )
        }

    fun findNavnSistOppdatert(orgnummer: Long): LocalDate = requireNotNull(using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "SELECT navn_oppdatert FROM arbeidsgiver_navn WHERE id=(SELECT navn_ref FROM arbeidsgiver WHERE orgnummer=?);",
                orgnummer
            ).map {
                it.localDate("navn_oppdatert")
            }.asSingle
        )
    })


    fun findArbeidsgiver(arbeidsgiverId: Long): ArbeidsgiverDto? = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "SELECT an.navn, a.orgnummer FROM arbeidsgiver AS a JOIN arbeidsgiver_navn AS an ON a.navn_ref = an.id WHERE a.id=?;",
                arbeidsgiverId
            ).map {
                ArbeidsgiverDto(
                    organisasjonsnummer = it.string("orgnummer"),
                    navn = it.string("navn")
                )
            }.asSingle
        )
    }

    fun updateNavn(orgnummer: String, navn: String) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "UPDATE arbeidsgiver_navn SET navn=? WHERE id(SELECT navn_ref FROM arbeidsgiver WHERE orgnummer=?, navn_oppdatert=?);",
                navn,
                orgnummer,
                LocalDateTime.now().minusYears(1)
            ).asUpdate
        )
    }
}
