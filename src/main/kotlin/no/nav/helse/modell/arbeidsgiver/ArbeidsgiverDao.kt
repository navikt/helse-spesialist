package no.nav.helse.modell.arbeidsgiver

import kotliquery.Session
import kotliquery.queryOf
import java.time.LocalDate
import java.time.LocalDateTime

internal fun Session.findArbeidsgiverByOrgnummer(orgnummer: Long): Int? = this.run(
    queryOf("SELECT id FROM arbeidsgiver WHERE orgnummer=?;", orgnummer)
        .map { it.int("id") }
        .asSingle
)

internal fun Session.insertArbeidsgiver(orgnummer: Long, navn: String): Long? {
    val navnRef = requireNotNull(
        run(
            queryOf(
                "INSERT INTO arbeidsgiver_navn(navn, navn_oppdatert) VALUES(?, ?);",
                navn,
                LocalDateTime.now().minusYears(1)
            )
                .asUpdateAndReturnGeneratedKey
        )
    )
    return run(
        queryOf("INSERT INTO arbeidsgiver(orgnummer, navn_ref) VALUES(?, ?);", orgnummer, navnRef)
            .asUpdateAndReturnGeneratedKey
    )
}

internal fun Session.findNavnSistOppdatert(orgnummer: Long): LocalDate = requireNotNull(
    this.run(
        queryOf(
            "SELECT navn_oppdatert FROM arbeidsgiver_navn WHERE id=(SELECT navn_ref FROM arbeidsgiver WHERE orgnummer=?);",
            orgnummer
        ).map {
            it.localDate("navn_oppdatert")
        }.asSingle
    )
)


internal fun Session.findArbeidsgiver(arbeidsgiverId: Int): ArbeidsgiverDto? = this.run(
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

internal fun Session.updateNavn(orgnummer: String, navn: String) = this.run(
    queryOf(
        "UPDATE arbeidsgiver_navn SET navn=?, navn_oppdatert=? WHERE id(SELECT navn_ref FROM arbeidsgiver WHERE orgnummer=?);",
        navn,
        LocalDateTime.now().minusYears(1),
        orgnummer
    ).asUpdate
)
