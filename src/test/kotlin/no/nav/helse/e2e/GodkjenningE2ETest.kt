package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import io.mockk.verify
import no.nav.helse.Oppgavestatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import no.nav.helse.api.GodkjenningDTO
import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.mediator.kafka.MiljøstyrtFeatureToggle
import no.nav.helse.mediator.kafka.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.Connection
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GodkjenningE2ETest : AbstractE2ETest() {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val ORGNR = "222222222"
        private const val SAKSBEHANDLERIDENT = "Z999999"
        private const val SAKSBEHANDLEREPOST = "saksbehandler@nav.no"
        private const val OPPGAVEID = 1L
        private val SAKSBEHANDLEROID = UUID.randomUUID()
        private const val SNAPSHOTV1 = """{"version": "this_is_version_1"}"""
        private const val SNAPSHOTV2 = """{"version": "this_is_version_2"}"""
    }

    @Test
    fun `ignorerer endringer på ukjente vedtaksperioder`() {
        val hendelseId = sendVedtaksperiodeEndret(ORGNR, VEDTAKSPERIODE_ID)
        assertHendelse(hendelseId)
        assertTilstand(hendelseId, "NY", "FERDIG")
        verify(exactly = 0) { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) }
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
    fun `oppretter vedtak ved godkjenningsbehov`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        assertSnapshot(SNAPSHOTV1, VEDTAKSPERIODE_ID)
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT")
        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler)
        assertVedtak(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler godkjenner`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        assertSnapshot(SNAPSHOTV1, VEDTAKSPERIODE_ID)
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "FERDIG")
        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovLøsning(true, SAKSBEHANDLERIDENT)
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler avslår`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, false)
        assertSnapshot(SNAPSHOTV1, VEDTAKSPERIODE_ID)
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "FERDIG")
        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovLøsning(false, SAKSBEHANDLERIDENT)
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
        assertHendelse(hendelseId)
        assertTilstand(hendelseId, "NY", "FERDIG")
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
}
