package no.nav.helse.modell

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class UtbetalingsgodkjenningMessageTest {
    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())

        private const val IDENT = "Z999999"
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
    }
    private lateinit var utbetalingMessage: UtbetalingsgodkjenningMessage

    @BeforeEach
    fun setup() {
        utbetalingMessage = UtbetalingsgodkjenningMessage("{}")
    }

    @Test
    fun `automatisk behandlet`() {
        utbetalingMessage.løsAutomatisk()
        assertGodkjent(true, "Automatisk behandlet")
    }

    @Test
    fun `manuelt godkjent`() {
        utbetalingMessage.løs(true, IDENT, GODKJENTTIDSPUNKT, null, null, null)
        assertGodkjent(false, IDENT, GODKJENTTIDSPUNKT)
    }

    @Test
    fun `manuelt avvist`() {
        utbetalingMessage.løs(false, IDENT, GODKJENTTIDSPUNKT, null, null, null)
        assertIkkeGodkjent(false, IDENT, GODKJENTTIDSPUNKT)
    }

    private fun assertGodkjent(automatisk: Boolean, ident: String, godkjenttidspunkt: LocalDateTime? = null) {
        assertMessage { løsning ->
            assertTrue(løsning.path("godkjent").booleanValue())
            assertLøsning(automatisk, ident, godkjenttidspunkt)
        }
    }

    private fun assertIkkeGodkjent(automatisk: Boolean, ident: String, godkjenttidspunkt: LocalDateTime? = null) {
        assertMessage { løsning ->
            assertFalse(løsning.path("godkjent").booleanValue())
            assertLøsning(automatisk, ident, godkjenttidspunkt)
        }
    }

    private fun assertLøsning(automatisk: Boolean, ident: String, godkjenttidspunkt: LocalDateTime? = null) {
        assertMessage { løsning ->
            assertEquals(automatisk, løsning.path("automatiskBehandling").booleanValue())
            assertEquals(ident, løsning.path("saksbehandlerIdent").asText())
            assertDoesNotThrow { løsning.path("godkjenttidspunkt").asLocalDateTime() }
            godkjenttidspunkt?.also { assertEquals(godkjenttidspunkt, løsning.path("godkjenttidspunkt").asLocalDateTime()) }
            assertTrue(løsning.path("årsak").isMissingOrNull())
            assertTrue(løsning.path("begrunnelser").isMissingOrNull())
            assertTrue(løsning.path("kommentar").isMissingOrNull())
        }
    }

    private fun assertMessage(block: (JsonNode) -> Unit) {
        objectMapper.readTree(utbetalingMessage.toJson())
            .path("@løsning")
            .path("Godkjenning")
            .apply(block)
    }
}
