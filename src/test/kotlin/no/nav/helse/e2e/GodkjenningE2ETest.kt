package no.nav.helse.e2e

import AbstractE2ETest
import com.fasterxml.jackson.databind.JsonNode
import io.mockk.every
import io.mockk.verify
import no.nav.helse.modell.Oppgavestatus
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.snapshot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GodkjenningE2ETest : AbstractE2ETest() {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val ORGNR = "222222222"
        private const val ENHET_UTLAND = "2101"
        private const val SAKSBEHANDLERIDENT = "Z999999"
        private const val SAKSBEHANDLEREPOST = "saksbehandler@nav.no"
        private const val OPPGAVEID = 1L
        private val SAKSBEHANDLEROID = UUID.randomUUID()
        private val SNAPSHOTV1 = snapshot(VEDTAKSPERIODE_ID)
        private val SNAPSHOTV2 = snapshot(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `ignorerer endringer på ukjente vedtaksperioder`() {
        val hendelseId = sendVedtaksperiodeEndret(ORGNR, VEDTAKSPERIODE_ID)
        assertIkkeHendelse(hendelseId)
        assertIkkeVedtak(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `oppretter ikke vedtak ved godkjenningsbehov uten nødvendig informasjon`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        assertHendelse(godkjenningsmeldingId)
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT")
        assertBehov("HentPersoninfo", "HentEnhet", "HentInfotrygdutbetalinger")
        assertIkkeVedtak(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `ignorerer behov uten tilhørende command`() {
        val hendelseId = UUID.randomUUID()
        val contextId = UUID.randomUUID()
        sendPersoninfoløsning(hendelseId, ORGNR, VEDTAKSPERIODE_ID, contextId)
        assertIkkeHendelse(hendelseId)
    }

    @Test
    fun `oppretter vedtak ved godkjenningsbehov`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendEgenAnsattløsning(godkjenningsmeldingId, false)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        assertSnapshot(SNAPSHOTV1, VEDTAKSPERIODE_ID)
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "FERDIG")
        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler)
        assertVedtak(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler godkjenner`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        val løsningId =
            sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        assertSnapshot(SNAPSHOTV1, VEDTAKSPERIODE_ID)
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "FERDIG")
        assertTilstand(løsningId, "NY", "FERDIG")
        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.AvventerSystem, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovløsning(true, SAKSBEHANDLERIDENT)
        assertNotNull(testRapid.inspektør.hendelser("vedtaksperiode_godkjent").firstOrNull())
    }

    @Test
    fun `slår sammen warnings fra spleis og spesialist i utgående event`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, warnings = listOf("En Warning"))
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId, 1
        )
        sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)

        val vedtaksperiodeGodkjentEvent = testRapid.inspektør.hendelser("vedtaksperiode_godkjent").firstOrNull()
        assertNotNull(vedtaksperiodeGodkjentEvent)
        assertEquals(UNG_PERSON_FNR_2018, vedtaksperiodeGodkjentEvent["fødselsnummer"].asText())
        assertTrue(vedtaksperiodeGodkjentEvent.hasNonNull("@opprettet"))
        assertEquals(2, vedtaksperiodeGodkjentEvent["warnings"].size())
        assertEquals(WarningKilde.Spleis.name, vedtaksperiodeGodkjentEvent["warnings"][0]["kilde"].asText())
        assertEquals(WarningKilde.Spesialist.name, vedtaksperiodeGodkjentEvent["warnings"][1]["kilde"].asText())
        assertFalse(vedtaksperiodeGodkjentEvent["automatiskBehandling"].asBoolean())
        assertVedtaksperiodeGodkjentEvent(vedtaksperiodeGodkjentEvent)
    }

    private fun assertVedtaksperiodeGodkjentEvent(vedtaksperiodeGodkjentEvent: JsonNode) {
        assertEquals(VEDTAKSPERIODE_ID, UUID.fromString(vedtaksperiodeGodkjentEvent["vedtaksperiodeId"].asText()))
        assertEquals(
            Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING.name,
            vedtaksperiodeGodkjentEvent["periodetype"].asText()
        )
        assertEquals(SAKSBEHANDLERIDENT, vedtaksperiodeGodkjentEvent["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLEREPOST, vedtaksperiodeGodkjentEvent["saksbehandlerEpost"].asText())
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler avslår`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        val løsningId =
            sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, false)
        assertSnapshot(SNAPSHOTV1, VEDTAKSPERIODE_ID)
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "FERDIG")
        assertTilstand(løsningId, "NY", "FERDIG")
        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.AvventerSystem, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovløsning(false, SAKSBEHANDLERIDENT)
    }

    @Test
    fun `endringer på kjente vedtaksperioder`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returnsMany listOf(SNAPSHOTV1, SNAPSHOTV2)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        val endringsmeldingId = sendVedtaksperiodeEndret(ORGNR, VEDTAKSPERIODE_ID)
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT")
        assertTilstand(endringsmeldingId, "NY", "FERDIG")
        assertSnapshot(SNAPSHOTV2, VEDTAKSPERIODE_ID)
        verify(exactly = 2) { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) }
    }

    @Test
    fun `vedtaksperiode forkastet`() {
        val hendelseId = sendVedtaksperiodeForkastet(ORGNR, VEDTAKSPERIODE_ID)
        assertIkkeHendelse(hendelseId)
        assertIkkeVedtak(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `gjør ingen ting om man får tilbake løsning på en avbrutt command context`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val hendelseId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(hendelseId, ORGNR, VEDTAKSPERIODE_ID)
        sendVedtaksperiodeForkastet(ORGNR, VEDTAKSPERIODE_ID)

        assertTilstand(hendelseId, "NY", "SUSPENDERT", "SUSPENDERT", "AVBRUTT")
        sendPersoninfoløsning(hendelseId, ORGNR, VEDTAKSPERIODE_ID)
        assertTilstand(hendelseId, "NY", "SUSPENDERT", "SUSPENDERT", "AVBRUTT")
    }

    @Test
    fun `oppretter ikke oppgave om bruker er egen ansatt`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendEgenAnsattløsning(godkjenningsmeldingId, true)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        assertSnapshot(SNAPSHOTV1, VEDTAKSPERIODE_ID)
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "FERDIG")
        assertVedtak(VEDTAKSPERIODE_ID)
        assertIngenOppgave()
        assertFalse(
            testRapid.inspektør.meldinger().first()
                .path("@løsning")
                .path("Godkjenning")
                .path("godkjent")
                .booleanValue()
        )
    }

    @Test
    fun `oppretter ikke oppgave om bruker tilhører utlandsenhet`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID, enhet = ENHET_UTLAND)
        sendEgenAnsattløsning(godkjenningsmeldingId, false)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        assertSnapshot(SNAPSHOTV1, VEDTAKSPERIODE_ID)
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "FERDIG")
        assertVedtak(VEDTAKSPERIODE_ID)
        assertIngenOppgave()
        assertFalse(
            testRapid.inspektør.meldinger().first()
                .path("@løsning")
                .path("Godkjenning")
                .path("godkjent")
                .booleanValue()
        )
    }
}
