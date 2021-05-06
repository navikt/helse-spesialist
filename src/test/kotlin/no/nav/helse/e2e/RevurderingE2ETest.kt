package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import junit.framework.Assert.assertTrue
import no.nav.helse.modell.oppgave.Oppgavestatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.snapshotMedWarning
import no.nav.helse.snapshotUtenWarnings
import org.junit.jupiter.api.Test
import java.util.*

internal class RevurderingE2ETest: AbstractE2ETest() {

    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val ORGNR = "222222222"
        private const val ENHET_UTLAND = "2101"
        private const val SAKSBEHANDLERIDENT = "Z999999"
        private const val AUTOMATISK_BEHANDLET = "Automatisk behandlet"
        private const val SAKSBEHANDLEREPOST = "saksbehandler@nav.no"

        private val SAKSBEHANDLEROID = UUID.randomUUID()
        private val SNAPSHOT_MED_WARNINGS = snapshotMedWarning(VEDTAKSPERIODE_ID)
        private val SNAPSHOT_UTEN_WARNINGS = snapshotUtenWarnings(VEDTAKSPERIODE_ID)
    }

    private val OPPGAVEID get() = testRapid.inspektør.oppgaveId()

    @Test
    fun `revurdering ved saksbehandlet oppgave`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOT_MED_WARNINGS //Legger på warning for at saken ikke skal automatiseres

        val godkjenningsmeldingId1 = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        håndterGodkjenningsbehov(godkjenningsmeldingId1)
        sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        sendUtbetalingEndret("UTBETALING", Utbetalingsstatus.UTBETALT, ORGNR, "EN_FAGSYSTEMID")
        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.AvventerSystem, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovløsning(true, SAKSBEHANDLERIDENT)

        val godkjenningsmeldingId2 = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID2) // revurdering
        håndterGodkjenningsbehov(godkjenningsmeldingId2)
        sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        sendUtbetalingEndret("UTBETALING", Utbetalingsstatus.UTBETALT, ORGNR, "EN_FAGSYSTEMID")
        assertOppgave(1, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.AvventerSystem, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovløsning(true, SAKSBEHANDLERIDENT)

    }


    @Test
    fun `revurdering ved automatisering`() {

    }

    private fun håndterGodkjenningsbehov(godkjenningsmeldingId: UUID) {
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
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
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
    }
}
