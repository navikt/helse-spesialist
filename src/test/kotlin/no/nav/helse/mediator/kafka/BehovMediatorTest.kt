package no.nav.helse.mediator.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.mediator.kafka.meldinger.Hendelse
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
    private companion object {
        private const val FNR = "fødselsnummer"
        private val hendelseId = UUID.randomUUID()
        private val contextId = UUID.randomUUID()
        private val vedtaksperiodeId = UUID.randomUUID()
        private val objectMapper = jacksonObjectMapper()
    }
    private val testRapid: TestRapid = TestRapid()
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    private val behovMediator: BehovMediator = BehovMediator(testRapid, sikkerLogg)
    private lateinit var testHendelse: TestHendelse
    private lateinit var testContext: CommandContext

    @BeforeEach
    fun setupEach() {
        testRapid.reset()
        testHendelse = TestHendelse(hendelseId)
        testContext = CommandContext(contextId)
    }

    @Test
    fun `sender behov`() {
        val params = mapOf(
            "param 1" to 1,
            "param 2" to 2
        )
        testContext.behov("type 1", params)
        behovMediator.håndter(testHendelse, testContext, contextId)
        assertEquals(listOf("type 1"), testRapid.inspektør.field(0, "@behov").map(JsonNode::asText))
        assertEquals("$contextId", testRapid.inspektør.field(0, "contextId").asText())
        assertEquals("$hendelseId", testRapid.inspektør.field(0, "hendelseId").asText())
        testRapid.inspektør.field(0, "type 1").also {
            assertEquals(1, it.path("param 1").asInt())
            assertEquals(2, it.path("param 2").asInt())
        }
    }

    @Test
    fun `sender meldinger`() {
        val melding1 = """{ "a_key": "with_a_value" }"""
        val melding2 = """{ "a_key": "with_a_value" }"""
        testContext.publiser(melding1)
        testContext.publiser(melding2)
        behovMediator.håndter(testHendelse, testContext, contextId)
        assertEquals(2, testRapid.inspektør.size)
        assertEquals(objectMapper.readTree(melding1), testRapid.inspektør.message(0))
        assertEquals(objectMapper.readTree(melding2), testRapid.inspektør.message(1))
    }

    @Test
    fun standardfelter() {
        testContext.behov("testbehov")
        behovMediator.håndter(testHendelse, testContext, contextId)
        assertEquals("behov", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals(FNR, testRapid.inspektør.field(0, "fødselsnummer").asText())
        assertDoesNotThrow { UUID.fromString(testRapid.inspektør.field(0, "@id").asText()) }
        assertDoesNotThrow { LocalDateTime.parse(testRapid.inspektør.field(0, "@opprettet").asText()) }
    }

    inner class TestHendelse(override val id: UUID) : Hendelse {
        override fun execute(context: CommandContext): Boolean {
            TODO("Not yet implemented")
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
