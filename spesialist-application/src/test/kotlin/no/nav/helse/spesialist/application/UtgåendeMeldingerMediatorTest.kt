package no.nav.helse.spesialist.application

import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.UtgåendeMeldingerMediator
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.modell.person.Person
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class UtgåendeMeldingerMediatorTest {
    private companion object {
        private const val FNR = "fødselsnummer"
        private val hendelseId = UUID.randomUUID()
        private val contextId = UUID.randomUUID()
        private val vedtaksperiodeId = UUID.randomUUID()
    }

    private val meldingPubliserer = object : MeldingPubliserer {
        var antallMeldinger: Int = 0
            private set

        override fun publiser(fødselsnummer: String, hendelse: UtgåendeHendelse, årsak: String) {
            antallMeldinger++
        }

        override fun publiser(fødselsnummer: String, subsumsjonEvent: SubsumsjonEvent, versjonAvKode: String) =
            error("Not implemented in test")

        override fun publiser(
            hendelseId: UUID,
            commandContextId: UUID,
            fødselsnummer: String,
            behov: List<Behov>
        ) = error("Not implemented in test")

        override fun publiser(fødselsnummer: String, event: KommandokjedeEndretEvent, hendelseNavn: String) = error("Not implemented in test")
    }

    private val utgåendeMeldingerMediator: UtgåendeMeldingerMediator = UtgåendeMeldingerMediator()
    private lateinit var testmelding: Testmelding
    private lateinit var testContext: CommandContext

    @BeforeEach
    fun setupEach() {
        testmelding = Testmelding(hendelseId)
        testContext = CommandContext(contextId)
        testContext.nyObserver(utgåendeMeldingerMediator)
    }

    @Test
    fun `sender meldinger`() {
        val hendelse1 = VedtaksperiodeGodkjentAutomatisk(
            fødselsnummer = lagFødselsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            behandlingId = UUID.randomUUID(),
            periodetype = "FØRSTEGANGSBEHANDLING"
        )
        val hendelse2 = VedtaksperiodeGodkjentAutomatisk(
            fødselsnummer = lagFødselsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            behandlingId = UUID.randomUUID(),
            periodetype = "FORLENGELSE"
        )
        testContext.hendelse(hendelse1)
        testContext.hendelse(hendelse2)
        utgåendeMeldingerMediator.publiserOppsamledeMeldinger(testmelding, meldingPubliserer)
        assertEquals(2, meldingPubliserer.antallMeldinger)
    }

    private inner class Testmelding(override val id: UUID) : Vedtaksperiodemelding {

        override fun fødselsnummer(): String {
            return FNR
        }

        override fun vedtaksperiodeId(): UUID {
            return vedtaksperiodeId
        }

        override fun behandle(
            person: Person,
            kommandostarter: Kommandostarter,
            sessionContext: SessionContext,
            syncPersonTilDatabase: () -> Unit,
        ) {
        }

        @Language("JSON")
        override fun toJson(): String {
            return """{ "@id": "${UUID.randomUUID()}", "@event_name": "testhendelse", "@opprettet": "${LocalDateTime.now()}" }"""
        }
    }
}
