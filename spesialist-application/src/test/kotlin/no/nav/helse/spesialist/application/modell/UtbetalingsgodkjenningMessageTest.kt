package no.nav.helse.spesialist.application.modell

import no.nav.helse.modell.melding.Godkjenningsbehovløsning
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.Testdata.godkjenningsbehovData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class UtbetalingsgodkjenningMessageTest {
    private companion object {

        private const val IDENT = "Z999999"
        private const val EPOST = "test@nav.no"
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
    }

    private lateinit var godkjenningsbehov: GodkjenningsbehovData
    private val utbetaling = Utbetaling(UUID.randomUUID(), 1000, 1000, Utbetalingtype.UTBETALING)

    @BeforeEach
    fun setup() {
        godkjenningsbehov = godkjenningsbehovData()
    }

    @Test
    fun `automatisk behandlet`() {
        godkjenningsbehov.godkjennAutomatisk(utbetaling)
        assertGodkjent(true, "Automatisk behandlet", "tbd@nav.no")
    }

    @Test
    fun `manuelt godkjent`() {
        godkjenningsbehov.godkjennManuelt(IDENT, EPOST, GODKJENTTIDSPUNKT, emptyList(), utbetaling)
        assertGodkjent(false, IDENT, EPOST, GODKJENTTIDSPUNKT)
    }

    @Test
    fun `manuelt avvist`() {
        godkjenningsbehov.avvisManuelt(IDENT, EPOST, GODKJENTTIDSPUNKT, null, null, null, emptyList(), utbetaling)
        assertMessage { løsning ->
            assertFalse(løsning.godkjent)
            assertLøsning(false, IDENT, EPOST, GODKJENTTIDSPUNKT)
        }
    }

    private fun assertGodkjent(
        automatisk: Boolean,
        ident: String,
        epost: String,
        godkjenttidspunkt: LocalDateTime? = null
    ) {
        assertMessage { løsning ->
            assertTrue(løsning.godkjent)
            assertLøsning(automatisk, ident, epost, godkjenttidspunkt)
        }
    }

    private fun assertLøsning(
        automatisk: Boolean,
        ident: String,
        epost: String,
        godkjenttidspunkt: LocalDateTime? = null
    ) {
        assertMessage { løsning ->
            assertEquals(automatisk, løsning.automatiskBehandling)
            assertEquals(ident, løsning.saksbehandlerIdent)
            assertEquals(epost, løsning.saksbehandlerEpost)
            if (godkjenttidspunkt != null) {
                assertEquals(godkjenttidspunkt, løsning.godkjenttidspunkt)
            }
            assertNull(løsning.årsak)
            assertNull(løsning.begrunnelser)
            assertNull(løsning.kommentar)
        }
    }

    private fun assertMessage(block: (Godkjenningsbehovløsning) -> Unit) {
        godkjenningsbehov.medLøsning()
            .apply(block)
    }
}
