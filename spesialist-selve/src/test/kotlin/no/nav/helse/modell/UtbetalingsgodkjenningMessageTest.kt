package no.nav.helse.modell

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtbetalingsgodkjenningMessageTest {
    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())

        private const val IDENT = "Z999999"
        private const val EPOST = "test@nav.no"
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
    }
    private lateinit var utbetalingMessage: UtbetalingsgodkjenningMessage
    private val utbetaling = Utbetaling(UUID.randomUUID(), 1000, 1000, Utbetalingtype.UTBETALING)

    @BeforeEach
    fun setup() {
        utbetalingMessage = UtbetalingsgodkjenningMessage("{}", utbetaling)
    }

    @Test
    fun `automatisk behandlet`() {
        utbetalingMessage.godkjennAutomatisk()
        assertGodkjent(true, "Automatisk behandlet", "tbd@nav.no")
    }

    @Test
    fun `manuelt godkjent`() {
        val behandlingId = UUID.randomUUID()
        utbetalingMessage.godkjennManuelt(behandlingId = behandlingId, IDENT, EPOST, GODKJENTTIDSPUNKT, emptyList())
        assertGodkjent(false, IDENT, EPOST, GODKJENTTIDSPUNKT, behandlingId)
    }

    @Test
    fun `manuelt avvist`() {
        val behandlingId = UUID.randomUUID()
        utbetalingMessage.avvisManuelt(behandlingId = behandlingId, IDENT, EPOST, GODKJENTTIDSPUNKT, null, null, null, emptyList())
        assertIkkeGodkjent(false, IDENT, EPOST, GODKJENTTIDSPUNKT, behandlingId)
    }

    private fun assertGodkjent(automatisk: Boolean, ident: String, epost: String, godkjenttidspunkt: LocalDateTime? = null, behandlingId: UUID? = null) {
        assertBehandlingId(behandlingId)
        assertMessage { løsning ->
            assertTrue(løsning.path("godkjent").booleanValue())
            assertLøsning(automatisk, ident, epost, godkjenttidspunkt)
        }
    }

    private fun assertIkkeGodkjent(
        automatisk: Boolean,
        ident: String,
        epost: String,
        godkjenttidspunkt: LocalDateTime? = null,
        behandlingId: UUID? = null
    ) {
        assertBehandlingId(behandlingId)
        assertMessage { løsning ->
            assertFalse(løsning.path("godkjent").booleanValue())
            assertLøsning(automatisk, ident, epost, godkjenttidspunkt)
        }
    }

    private fun assertBehandlingId(behandlingId: UUID?) {
        val message = objectMapper.readTree(utbetalingMessage.toJson())
        val faktiskBehandlingId = message["behandlingId"].textValue()
        if (behandlingId != null) assertEquals(behandlingId.toString(), faktiskBehandlingId)
        else assertNotNull(faktiskBehandlingId)
    }

    private fun assertLøsning(automatisk: Boolean, ident: String, epost: String, godkjenttidspunkt: LocalDateTime? = null) {
        assertMessage { løsning ->
            assertEquals(automatisk, løsning.path("automatiskBehandling").booleanValue())
            assertEquals(ident, løsning.path("saksbehandlerIdent").asText())
            assertEquals(epost, løsning.path("saksbehandlerEpost").asText())
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
