package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID.randomUUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class SaksbehandlerløsningTest {

    private companion object {
        private val GODKJENNINGSBEHOV_ID = randomUUID()
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
        private const val OPPGAVE_ID = 1L
        private const val FNR = "12020052345"
        private const val IDENT = "Z999999"
        private const val HENDELSE_JSON = """{ "this_key_should_exist": "this_value_should_exist" }"""
        private const val GODKJENNINGSBEHOV_JSON = """{ "@event_name": "behov" }"""
        private val objectMapper = jacksonObjectMapper()
    }

    private val hendelseDao = mockk<HendelseDao>(relaxed = true)

    private val godkjenningsbehov = UtbetalingsgodkjenningMessage(GODKJENNINGSBEHOV_JSON)
    private fun saksbehandlerløsning(godkjent: Boolean) = Saksbehandlerløsning(
        id = randomUUID(),
        godkjenningsbehovhendelseId = GODKJENNINGSBEHOV_ID,
        fødselsnummer = FNR,
        godkjent = godkjent,
        saksbehandlerIdent = IDENT,
        oid = randomUUID(),
        epostadresse = "saksbehandler@nav.no",
        godkjenttidspunkt = GODKJENTTIDSPUNKT,
        årsak = null,
        begrunnelser = null,
        kommentar = null,
        oppgaveId = OPPGAVE_ID,
        json = HENDELSE_JSON,
        oppgaveDao = mockk(relaxed = true),
        hendelseDao = hendelseDao,
        godkjenningMediator = GodkjenningMediator(mockk(relaxed = true), mockk(relaxed = true), mockk()),
    )

    private val context = CommandContext(randomUUID())

    @Test
    fun `løser godkjenningsbehov`() {
        every { hendelseDao.finnUtbetalingsgodkjenningbehov(GODKJENNINGSBEHOV_ID) } returns godkjenningsbehov
        val saksbehandlerløsning = saksbehandlerløsning(true)
        assertTrue(saksbehandlerløsning.execute(context))
        assertLøsning(true)
    }

    @Test
    fun `løser godkjenningsbehov ved avvist utbetaling`() {
        every { hendelseDao.finnUtbetalingsgodkjenningbehov(GODKJENNINGSBEHOV_ID) } returns godkjenningsbehov
        val saksbehandlerløsning = saksbehandlerløsning(false)
        assertTrue(saksbehandlerløsning.execute(context))
        assertLøsning(false)
    }

    private fun assertLøsning(godkjent: Boolean) {
        val løsning = context.meldinger()
            .map(objectMapper::readTree)
            .filter { it["@event_name"].asText() == "behov" }
            .firstOrNull { it["@løsning"].hasNonNull("Godkjenning") } ?: fail("Fant ikke løsning på godkjenningsbehov")

        assertJsonEquals(GODKJENNINGSBEHOV_JSON, løsning)

        val godkjenning = løsning.path("@løsning").path("Godkjenning")
        assertTrue(godkjenning.path("godkjent").isBoolean)
        assertEquals(godkjent, godkjenning.path("godkjent").booleanValue())
        assertEquals(IDENT, godkjenning.path("saksbehandlerIdent").textValue())
        assertEquals(GODKJENTTIDSPUNKT, LocalDateTime.parse(godkjenning.path("godkjenttidspunkt").textValue()))
        assertTrue(godkjenning.path("årsak").isNull)
        assertTrue(godkjenning.path("kommentar").isNull)
        assertTrue(godkjenning.path("begrunnelser").isNull)
    }

    private fun assertJsonEquals(expected: String, actual: JsonNode) {
        val expectedJson = objectMapper.readTree(expected)
        assertJsonEquals(expectedJson, actual)
    }

    private fun assertJsonEquals(field: String, expected: JsonNode, actual: JsonNode) {
        assertEquals(
            expected.nodeType,
            actual.nodeType
        ) { "Field <$field> was not of expected value. Expected <${expected.nodeType}> got <${actual.nodeType}>" }
        when (expected.nodeType) {
            JsonNodeType.OBJECT -> assertJsonEquals(expected, actual)
            else -> assertEquals(
                expected,
                actual
            ) { "Field <$field> was not of expected value. Expected <${expected}> got <${actual}>" }
        }
    }

    private fun assertJsonEquals(expected: JsonNode, actual: JsonNode) {
        expected.fieldNames().forEach { field ->
            assertTrue(actual.has(field)) { "Expected field <$field> to exist" }
            assertJsonEquals(field, expected.path(field), actual.path(field))
        }
    }
}
