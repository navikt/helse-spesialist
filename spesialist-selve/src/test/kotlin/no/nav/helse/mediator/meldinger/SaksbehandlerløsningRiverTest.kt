package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.løsninger.SaksbehandlerløsningRiver
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class SaksbehandlerløsningRiverTest {
    private companion object {
        private val ID = UUID.randomUUID()
        private val GODKJENNINGSBEHOV_ID = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
        private const val FNR = "12345678911"
    }

    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(SaksbehandlerløsningRiver(mediator))

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser saksbehandlerløsning`() {
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagSaksbehandlerløsning(
                FNR,
                GODKJENNINGSBEHOV_ID,
                CONTEXT,
                id = ID
            )
        )
        verify(exactly = 1) { mediator.mottaMelding(any<Saksbehandlerløsning>(), any()) }
    }

    @Test
    fun `leser saksbehandlerløsning med saksbehandler og beslutter`() {
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagSaksbehandlerløsning(
                FNR,
                GODKJENNINGSBEHOV_ID,
                CONTEXT,
                id = ID,
                saksbehandlerepost = "saksbehandler@nav.no",
                saksbehandlerident = "saksbehandlerident",
                beslutterepost = "beslutter@nav.no",
                beslutterident = "beslutterident"
            )
        )
        verify(exactly = 1) {
            mediator.mottaMelding(
                withArg<Saksbehandlerløsning> {
                    assertEquals("saksbehandler@nav.no", it.saksbehandler.epostadresse)
                    assertEquals("saksbehandlerident", it.saksbehandler.ident)
                    assertEquals("beslutter@nav.no", it.beslutter?.epostadresse)
                    assertEquals("beslutterident", it.beslutter?.ident)
                },
                any()
            )
        }
    }
}
