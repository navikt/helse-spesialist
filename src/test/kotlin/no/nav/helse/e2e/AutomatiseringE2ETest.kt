package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.junit.jupiter.api.*
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AutomatiseringE2ETest : AbstractE2ETest() {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val ORGNR = "222222222"
        private const val SAKSBEHANDLERIDENT = "Z999999"
        private const val SAKSBEHANDLEREPOST = "saksbehandler@nav.no"
        private const val OPPGAVEID = 1L
        private val SAKSBEHANDLEROID = UUID.randomUUID()
        private const val SNAPSHOTV1 = """{"version": "this_is_version_1"}"""
    }

    @BeforeAll
    fun setup() {
        every { miljøstyrtFeatureToggle.risikovurdering() }.returns(true)
        every { miljøstyrtFeatureToggle.automatisering() }.returns(true)
    }

    @AfterAll
    fun tearDown() {
        every { miljøstyrtFeatureToggle.risikovurdering() }.returns(false)
        every { miljøstyrtFeatureToggle.automatisering() }.returns(false)
    }

    @Test
    fun `fatter automatisk vedtak`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            periodetype = Saksbehandleroppgavetype.FORLENGELSE
        )
        sendPersoninfoløsning(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "FERDIG")
        assertAutomatisertLøsning()
    }

    @Test
    fun `fatter ikke automatisk vedtak ved warnings`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            warnings = listOf("WARNING"),
            periodetype = Saksbehandleroppgavetype.FORLENGELSE
        )
        sendPersoninfoløsning(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendSaksbehandlerløsning(
            oppgaveId = OPPGAVEID,
            saksbehandlerIdent = SAKSBEHANDLERIDENT,
            saksbehandlerEpost = SAKSBEHANDLEREPOST,
            saksbehandlerOid = SAKSBEHANDLEROID,
            godkjent = true
        )
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "FERDIG")
        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.AvventerSystem, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovLøsning(godkjent = true, saksbehandlerIdent = SAKSBEHANDLERIDENT)
    }

    @Test
    fun `fatter ikke automatisk vedtak for førstegangsbehandling`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            periodetype = Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
        )
        sendPersoninfoløsning(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendSaksbehandlerløsning(
            oppgaveId = OPPGAVEID,
            saksbehandlerIdent = SAKSBEHANDLERIDENT,
            saksbehandlerEpost = SAKSBEHANDLEREPOST,
            saksbehandlerOid = SAKSBEHANDLEROID,
            godkjent = true
        )
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "FERDIG")
        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.AvventerSystem, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovLøsning(godkjent = true, saksbehandlerIdent = SAKSBEHANDLERIDENT)
    }

    @Test
    fun `fatter ikke automatisk vedtak ved 8-4 ikke oppfylt`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID,
            periodetype = Saksbehandleroppgavetype.FORLENGELSE
        )
        sendPersoninfoløsning(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendRisikovurderingløsning(
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            godkjenningsmeldingId = godkjenningsmeldingId,
            begrunnelser = listOf("8-4 ikke oppfylt")

        )
        sendSaksbehandlerløsning(
            oppgaveId = OPPGAVEID,
            saksbehandlerIdent = SAKSBEHANDLERIDENT,
            saksbehandlerEpost = SAKSBEHANDLEREPOST,
            saksbehandlerOid = SAKSBEHANDLEROID,
            godkjent = true
        )
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "FERDIG")
        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.AvventerSystem, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovLøsning(godkjent = true, saksbehandlerIdent = SAKSBEHANDLERIDENT)
        assertWarning("Arbeidsuførhet, aktivitetsplikt og/eller medvirkning må vurderes", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `fatter ikke automatisk vedtak når bruker er ikke-digital`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            periodetype = Saksbehandleroppgavetype.FORLENGELSE
        )
        sendPersoninfoløsning(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = false
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendSaksbehandlerløsning(
            oppgaveId = OPPGAVEID,
            saksbehandlerIdent = SAKSBEHANDLERIDENT,
            saksbehandlerEpost = SAKSBEHANDLEREPOST,
            saksbehandlerOid = SAKSBEHANDLEROID,
            godkjent = true
        )
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "FERDIG")
        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.AvventerSystem, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovLøsning(godkjent = true, saksbehandlerIdent = SAKSBEHANDLERIDENT)
    }

    private fun assertAutomatisertLøsning() {
        assertLøsning("Godkjenning") {
            Assertions.assertTrue(it.path("godkjent").isBoolean)
            Assertions.assertTrue(it.path("godkjent").booleanValue())
            Assertions.assertTrue(it.path("automatiskBehandling").booleanValue())
            Assertions.assertEquals("Automatisk behandlet", it.path("saksbehandlerIdent").textValue())
            Assertions.assertNotNull(it.path("godkjenttidspunkt").asLocalDateTime())
        }
    }
}
