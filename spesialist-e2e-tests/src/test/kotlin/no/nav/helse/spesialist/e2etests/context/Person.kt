package no.nav.helse.spesialist.e2etests.context

import no.nav.helse.spesialist.domain.testfixtures.testdata.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsdato
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagMellomnavnOrNull
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDate

data class Person(
    val fødselsdato: LocalDate = lagFødselsdato(),
    val kjønn: Kjønn = Kjønn.entries.toTypedArray().random(),
    val fødselsnummer: String = lagFødselsnummer(fødselsdato = fødselsdato, mann = kjønn == Kjønn.Mann),
    val aktørId: String = lagAktørId(),

    val fornavn: String = lagFornavn(),
    val mellomnavn: String? = lagMellomnavnOrNull(),
    val etternavn: String = lagEtternavn()
)
