package no.nav.helse.spesialist.e2etests.context

import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.lagMellomnavnOrNull
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

data class VårTestPerson(
    val fødselsdato: LocalDate = LocalDate.now().minusYears(18).minusDays(Random.nextLong(until = 365 * 82)),
    val fødselsnummer: String = fødselsdato.format(DateTimeFormatter.ofPattern("ddMMyy00000")),
    val aktørId: String = lagAktørId(),

    val fornavn: String = lagFornavn(),
    val mellomnavn: String? = lagMellomnavnOrNull(),
    val etternavn: String = lagEtternavn(),
    val kjønn: Kjønn = Kjønn.entries.toTypedArray().random()
)
