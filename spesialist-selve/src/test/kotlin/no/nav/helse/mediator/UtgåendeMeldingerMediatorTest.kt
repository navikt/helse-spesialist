package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import kotliquery.TransactionalSession
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.Person
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.spesialist.test.lagFødselsnummer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class UtgåendeMeldingerMediatorTest {
    private companion object {
        private const val FNR = "fødselsnummer"
        private val hendelseId = UUID.randomUUID()
        private val contextId = UUID.randomUUID()
        private val vedtaksperiodeId = UUID.randomUUID()
    }

    private val testRapid: TestRapid = TestRapid()
    private val utgåendeMeldingerMediator: UtgåendeMeldingerMediator = UtgåendeMeldingerMediator()
    private lateinit var testmelding: Testmelding
    private lateinit var testContext: CommandContext

    @BeforeEach
    fun setupEach() {
        testRapid.reset()
        testmelding = Testmelding(hendelseId)
        testContext = CommandContext(contextId)
        testContext.nyObserver(utgåendeMeldingerMediator)
    }

    @Test
    fun `sender behov`() {
        val fom = LocalDate.now().minusDays(1)
        val tom = LocalDate.now()
        testContext.behov(Behov.Infotrygdutbetalinger(fom, tom))
        utgåendeMeldingerMediator.publiserOppsamledeMeldinger(testmelding, testRapid)
        assertTrue(!testRapid.inspektør.field(0, "@behov").isMissingOrNull())
        assertEquals("behov", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals(FNR, testRapid.inspektør.field(0, "fødselsnummer").asText())
        assertEquals(contextId.toString(), testRapid.inspektør.field(0, "contextId").asText())
        assertEquals(hendelseId.toString(), testRapid.inspektør.field(0, "hendelseId").asText())
        assertDoesNotThrow { UUID.fromString(testRapid.inspektør.field(0, "@id").asText()) }
        assertDoesNotThrow { LocalDateTime.parse(testRapid.inspektør.field(0, "@opprettet").asText()) }
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
        utgåendeMeldingerMediator.publiserOppsamledeMeldinger(testmelding, testRapid)
        assertEquals(2, testRapid.inspektør.size)
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
            transactionalSession: TransactionalSession
        ) {
        }

        @Language("JSON")
        override fun toJson(): String {
            return """{ "@id": "${UUID.randomUUID()}", "@event_name": "testhendelse", "@opprettet": "${LocalDateTime.now()}" }"""
        }
    }
}
