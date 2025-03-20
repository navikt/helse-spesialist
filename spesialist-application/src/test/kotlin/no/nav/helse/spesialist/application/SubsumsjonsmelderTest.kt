package no.nav.helse.spesialist.application

import no.nav.helse.MeldingPubliserer
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class SubsumsjonsmelderTest {
    private val meldingPubliserer = object : MeldingPubliserer {
        var antallMeldinger: Int = 0
            private set

        override fun publiser(fødselsnummer: String, hendelse: UtgåendeHendelse, årsak: String) =
            error("Not implemented for test")

        override fun publiser(fødselsnummer: String, subsumsjonEvent: SubsumsjonEvent, versjonAvKode: String) {
            antallMeldinger++
        }

        override fun publiser(
            hendelseId: UUID,
            commandContextId: UUID,
            fødselsnummer: String,
            behov: List<Behov>
        ) = error("Not implemented for test")

        override fun publiser(fødselsnummer: String, event: KommandokjedeEndretEvent, hendelseNavn: String) = error("Not implemented for test")
    }

    private val subsumsjonsmelder = Subsumsjonsmelder("versjonAvKode", meldingPubliserer)

    @Test
    fun `publiserer en subsumsjon`() {
        val fødselsnummer = lagFødselsnummer()
        val subsumsjonEvent = SubsumsjonEvent(
            id = UUID.randomUUID(),
            fødselsnummer = fødselsnummer,
            paragraf = "EN PARAGRAF",
            ledd = "ET LEDD",
            bokstav = "EN BOKSTAV",
            lovverk = "folketrygdloven",
            lovverksversjon = "1970-01-01",
            utfall = "VILKAR_BEREGNET",
            input = mapOf("foo" to "bar"),
            output = mapOf("foo" to "bar"),
            sporing = mapOf("identifikator" to listOf("EN ID")),
            tidsstempel = LocalDateTime.now(),
            kilde = "KILDE",
        )

        subsumsjonsmelder.nySubsumsjon(fødselsnummer, subsumsjonEvent)

        assertEquals(1, meldingPubliserer.antallMeldinger)
    }
}
