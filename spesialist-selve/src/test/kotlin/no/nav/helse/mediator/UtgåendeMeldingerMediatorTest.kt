package no.nav.helse.mediator

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.TransactionalSession
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.behov.Behov
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.Person
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
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
        private val objectMapper = jacksonObjectMapper()
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
        val melding1 = """{ "a_key": "with_a_value" }"""
        val melding2 = """{ "a_key": "with_a_value" }"""
        testContext.publiser(melding1)
        testContext.publiser(melding2)
        utgåendeMeldingerMediator.publiserOppsamledeMeldinger(testmelding, testRapid)
        assertEquals(2, testRapid.inspektør.size)
        assertEquals(objectMapper.readTree(melding1), testRapid.inspektør.message(0))
        assertEquals(objectMapper.readTree(melding2), testRapid.inspektør.message(1))
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
