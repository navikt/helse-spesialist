package no.nav.helse.spesialist.e2etests.context

import no.nav.helse.spesialist.domain.testfixtures.testdata.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagMellomnavnOrNull
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import kotlin.random.nextInt

data class Person(
    val fødselsdato: LocalDate = lagFødselsdatoForAlder(fraAlder = 18, tilAlder = 100),
    val fødselsnummer: String = fødselsdato.format(DateTimeFormatter.ofPattern("ddMMyy00000")),
    val aktørId: String = lagAktørId(),

    val fornavn: String = lagFornavn(),
    val mellomnavn: String? = lagMellomnavnOrNull(),
    val etternavn: String = lagEtternavn(),
    val kjønn: Kjønn = Kjønn.entries.toTypedArray().random()
)

private fun lagFødselsdatoForAlder(fraAlder: Int, tilAlder: Int): LocalDate =
    LocalDate.now()
        .minusYears(1 + Random.nextInt(fraAlder..(tilAlder)).toLong())
        .plusDays(Random.nextInt(1..364).toLong())
