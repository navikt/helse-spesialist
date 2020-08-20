package no.nav.helse.mediator.kafka

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.kafka.meldinger.Hendelse
import no.nav.helse.mediator.kafka.meldinger.ICommandMediator
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

internal class BehovMediatorTest {
    private val testRapid: TestRapid = TestRapid()
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    private val behovMediator: BehovMediator = BehovMediator(testRapid, sikkerLogg)
    private lateinit var testHendelse: TestHendelse
    private lateinit var testContext: CommandContext
    private val hendelseId = UUID.randomUUID()
    private val contextId = UUID.randomUUID()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val FNR = "fødselsnummer"

    @BeforeEach
    fun setupEach() {
        testRapid.reset()
        testHendelse = TestHendelse(hendelseId)
        testContext = CommandContext(contextId)
    }

    @Test
    fun `sender behov på kafka`() {
        val params = mapOf(
            "param 1" to 1,
            "param 2" to 2
        )
        testContext.behov("type 1", params)
        behovMediator.håndter(testHendelse, testContext)
        assertEquals(listOf("type 1"), testRapid.inspektør.field(0, "@behov").map(JsonNode::asText))
        assertEquals("$contextId", testRapid.inspektør.field(0, "contextId").asText())
        assertEquals("$hendelseId", testRapid.inspektør.field(0, "hendelseId").asText())
        testRapid.inspektør.field(0, "type 1").also {
            assertEquals(1, it.path("param 1").asInt())
            assertEquals(2, it.path("param 2").asInt())
        }
    }

    @Test
    fun `standardfelter`() {
        testContext.behov("testbehov")
        behovMediator.håndter(testHendelse, testContext)
        assertEquals("behov", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals(FNR, testRapid.inspektør.field(0, "fødselsnummer").asText())
        assertDoesNotThrow { UUID.fromString(testRapid.inspektør.field(0, "@id").asText()) }
        assertDoesNotThrow { LocalDateTime.parse(testRapid.inspektør.field(0, "@opprettet").asText()) }
    }

    inner class TestHendelse(override val id: UUID) : Hendelse {
        override fun håndter(mediator: ICommandMediator, context: CommandContext) {
            throw UnsupportedOperationException()
        }

        override fun fødselsnummer(): String {
            return FNR
        }

        override fun vedtaksperiodeId(): UUID {
            return vedtaksperiodeId
        }

        override fun toJson(): String {
            throw UnsupportedOperationException()
        }
    }
}
