package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import no.nav.helse.abonnement.OpptegnelseType
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.oppgave.Oppgavestatus
import no.nav.helse.overstyring.Dagtype
import no.nav.helse.overstyring.OverstyringDagDto
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals

internal class RevurderingE2ETest : AbstractE2ETest() {

    private companion object {
        private const val SAKSBEHANDLERIDENT = "Z999999"
        private const val SAKSBEHANDLEREPOST = "saksbehandler@nav.no"

        private val SAKSBEHANDLEROID = UUID.randomUUID()
    }

    private val OPPGAVEID get() = testRapid.inspektør.oppgaveId()

    @BeforeEach
    fun setup() {
        every { restClient.hentSpeilSpapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_UTEN_WARNINGS
    }

    @Test
    fun `revurdering ved saksbehandlet oppgave`() {
        every { restClient.hentSpeilSpapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_MED_WARNINGS //Legger på warning for at saken ikke skal automatiseres

        val godkjenningsmeldingId1 = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        håndterGodkjenningsbehov(godkjenningsmeldingId1)
        sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        sendUtbetalingEndret(
            "UTBETALING",
            Utbetalingsstatus.UTBETALT,
            ORGNR,
            "EN_FAGSYSTEMID",
            utbetalingId = UTBETALING_ID
        )
        assertOppgavestatuser(
            0,
            Oppgavestatus.AvventerSaksbehandler,
            Oppgavestatus.AvventerSystem,
            Oppgavestatus.Ferdigstilt
        )
        assertOppgavetype(0, "SØKNAD")
        assertGodkjenningsbehovløsning(true, SAKSBEHANDLERIDENT)

        val godkjenningsmeldingId2 = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID2,
            utbetalingtype = Utbetalingtype.REVURDERING
        )
        håndterGodkjenningsbehov(godkjenningsmeldingId2)
        sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        sendUtbetalingEndret(
            "REVURDERING",
            Utbetalingsstatus.UTBETALT,
            ORGNR,
            "EN_FAGSYSTEMID",
            utbetalingId = UTBETALING_ID2
        )
        assertOppgavestatuser(
            1,
            Oppgavestatus.AvventerSaksbehandler,
            Oppgavestatus.AvventerSystem,
            Oppgavestatus.Ferdigstilt
        )
        assertOppgavetype(1, "REVURDERING")
        assertGodkjenningsbehovløsning(true, SAKSBEHANDLERIDENT)
    }

    @Test
    fun `revurdering av periode medfører oppgave selv om perioden ikke har warnings`() {
        val godkjenningsmeldingId1 = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        håndterGodkjenningsbehov(godkjenningsmeldingId1)
        sendUtbetalingEndret(
            "UTBETALING",
            Utbetalingsstatus.UTBETALT,
            ORGNR,
            "EN_FAGSYSTEMID",
            utbetalingId = UTBETALING_ID
        )

        val godkjenningsmeldingId2 = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID2,
            utbetalingtype = Utbetalingtype.REVURDERING
        )
        håndterGodkjenningsbehov(godkjenningsmeldingId2)
        sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        sendUtbetalingEndret(
            "REVURDERING",
            Utbetalingsstatus.UTBETALT,
            ORGNR,
            "EN_FAGSYSTEMID",
            utbetalingId = UTBETALING_ID2
        )
        assertOppgavestatuser(
            0,
            Oppgavestatus.AvventerSaksbehandler,
            Oppgavestatus.AvventerSystem,
            Oppgavestatus.Ferdigstilt
        )
        assertOppgavetype(0, "REVURDERING")
        assertGodkjenningsbehovløsning(true, SAKSBEHANDLERIDENT)
    }

    @Test
    fun `fanger opp og informerer saksbehandler om avvist revurdering`() {
        val godkjenningsmeldingId1 = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)

        håndterGodkjenningsbehov(godkjenningsmeldingId1)

        sendOverstyrteDager(listOf(OverstyringDagDto(LocalDate.now(), Dagtype.Feriedag, null)))

        // Behind the scenes: Saksbehandler har også hooket opp en opptegnelse
        speilOppretterAbonnement()
        sendRevurderingAvvist(FØDSELSNUMMER, listOf("Revurderingen er åpenbart helt feil"))

        val opptegnelser = opptegnelseApiDao.finnOpptegnelser(SAKSBEHANDLER_OID)
        assertEquals(1, opptegnelser.size)
        assertEquals(OpptegnelseType.REVURDERING_AVVIST, opptegnelser.first().type)
        assertEquals(AKTØR.toLong(), opptegnelser.first().aktørId)
        assertTrue(opptegnelser.first().payload.contains("Revurderingen er åpenbart helt feil"))
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

    private fun speilOppretterAbonnement() {
        abonnementDao.opprettAbonnement(SAKSBEHANDLER_OID, AKTØR.toLong())
    }
}
