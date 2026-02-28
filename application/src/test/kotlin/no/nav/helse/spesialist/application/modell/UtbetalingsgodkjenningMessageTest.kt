package no.nav.helse.spesialist.application.modell

import no.nav.helse.modell.melding.Godkjenningsbehovløsning
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.Testdata.godkjenningsbehovData
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class UtbetalingsgodkjenningMessageTest {
    private companion object {
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
    }

    private val saksbehandler = lagSaksbehandler()

    private lateinit var godkjenningsbehov: GodkjenningsbehovData

    @BeforeEach
    fun setup() {
        godkjenningsbehov = godkjenningsbehovData()
    }

    @Test
    fun `automatisk behandlet`() {
        godkjenningsbehov.godkjennAutomatisk()
        assertGodkjent(true, "Automatisk behandlet", "tbd@nav.no")
    }

    @Test
    fun `manuelt godkjent`() {
        godkjenningsbehov.godkjennManuelt(saksbehandler.ident, saksbehandler.epost, GODKJENTTIDSPUNKT, emptyList())
        assertGodkjent(false, saksbehandler.ident.value, saksbehandler.epost, GODKJENTTIDSPUNKT)
    }

    @Test
    fun `manuelt avvist`() {
        godkjenningsbehov.avvisManuelt(saksbehandler.ident, saksbehandler.epost, GODKJENTTIDSPUNKT, null, null, null, emptyList())
        assertMessage { løsning ->
            assertFalse(løsning.godkjent)
            assertLøsning(false, saksbehandler.ident.value, saksbehandler.epost, GODKJENTTIDSPUNKT)
        }
    }

    private fun assertGodkjent(
        automatisk: Boolean,
        ident: String,
        epost: String,
        godkjenttidspunkt: LocalDateTime? = null,
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
        godkjenttidspunkt: LocalDateTime? = null,
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
        godkjenningsbehov
            .medLøsning()
            .apply(block)
    }
}
