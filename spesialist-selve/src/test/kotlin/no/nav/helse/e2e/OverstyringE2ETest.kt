package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import no.nav.helse.januar
import no.nav.helse.mediator.FeatureToggle
import no.nav.helse.oppgave.OppgaveDto
import no.nav.helse.overstyring.Dagtype
import no.nav.helse.overstyring.OverstyringDagDto
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class OverstyringE2ETest : AbstractE2ETest() {

    private fun List<OppgaveDto>.ingenOppgaveMedId(id: String) = none { it.oppgavereferanse == id }
    private fun assertIngenOppgaver(id: String) {
        oppgaveDao.finnOppgaver(false).ingenOppgaveMedId(id)
    }

    @Test
    fun `saksbehandler overstyrer sykdomstidslinje`() {
        val originaltGodkjenningsbehov = settOppBruker()

        sendOverstyrteDager(
            listOf(
                OverstyringDagDto(
                    dato = LocalDate.of(2018, 1, 20),
                    type = Dagtype.Feriedag,
                    grad = null
                )
            )
        )

        assertTrue(overstyringApiDao.finnOverstyringerAvTidslinjer(FØDSELSNUMMER, ORGNR).isNotEmpty())
        val originalOppgaveId = testRapid.inspektør.oppgaveId(originaltGodkjenningsbehov)
        assertIngenOppgaver(originalOppgaveId)

        val nyttGodkjenningsbehov = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID,
            UTBETALING_ID,
            LocalDate.of(2018, 1, 1),
            LocalDate.of(2018, 1, 31)
        )

        klargjørForGodkjenning(nyttGodkjenningsbehov)

        val oppgave = oppgaveDao.finnOppgaver(false).find { it.fødselsnummer == FØDSELSNUMMER }
        assertEquals(SAKSBEHANDLER_EPOST, oppgave!!.tildeling?.epost)
    }

    @Test
    fun `saksbehandler overstyrer inntekt`() {
        FeatureToggle.Toggle("overstyr_inntekt").enable()
        val godkjenningsbehovId = settOppBruker()
        val hendelseId = sendOverstyrtInntekt(månedligInntekt = 25000.0, skjæringstidspunkt = 1.januar)

        val overstyringer = overstyringApiDao.finnOverstyringerAvInntekt(FØDSELSNUMMER, ORGNR)
        assertEquals(1, overstyringer.size)
        assertEquals(FØDSELSNUMMER, overstyringer.first().fødselsnummer)
        assertEquals(ORGNR, overstyringer.first().organisasjonsnummer)
        assertEquals(hendelseId, overstyringer.first().hendelseId)
        assertEquals("saksbehandlerIdent", overstyringer.first().saksbehandlerIdent)
        assertEquals("saksbehandler", overstyringer.first().saksbehandlerNavn)
        assertEquals(25000.0, overstyringer.first().månedligInntekt)
        assertEquals(1.januar, overstyringer.first().skjæringstidspunkt)
        assertEquals("begrunnelse", overstyringer.first().begrunnelse)

        assertEquals(1, overstyringApiDao.finnOverstyringerAvInntekt(FØDSELSNUMMER, ORGNR).size)

        assertIngenOppgaver(testRapid.inspektør.oppgaveId(godkjenningsbehovId))

        val nyttGodkjenningsbehov = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            periodeFom = 1.januar,
            periodeTom = 31.januar,
            skjæringstidspunkt = 1.januar
        )

        klargjørForGodkjenning(nyttGodkjenningsbehov)

        val oppgave = requireNotNull(oppgaveDao.finnOppgaver(false).find { it.fødselsnummer == FØDSELSNUMMER })
        assertEquals(SAKSBEHANDLER_EPOST, oppgave.tildeling?.epost)
        FeatureToggle.Toggle("overstyr_inntekt").disable()
    }

    @Test
    fun `legger ved overstyringer i speil snapshot`() {
        val hendelseId = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID,
            UTBETALING_ID,
            LocalDate.of(2018, 1, 1),
            LocalDate.of(2018, 1, 31)
        )
        every { restClient.hentSpeilSpapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_MED_WARNINGS
        sendPersoninfoløsning(hendelseId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = hendelseId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            navn = "En Arbeidsgiver",
            bransjer = listOf("Bransje1", "Bransje2")
        )
        sendArbeidsforholdløsning(
            hendelseId = hendelseId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(hendelseId, false)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = hendelseId,
            erDigital = true
        )
        sendOverstyrteDager(
            listOf(
                OverstyringDagDto(
                    dato = LocalDate.of(2018, 1, 20),
                    type = Dagtype.Feriedag,
                    grad = null
                )
            )
        )

        val hendelseId2 = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID,
            UTBETALING_ID,
            LocalDate.of(2018, 1, 1),
            LocalDate.of(2018, 1, 31)
        )
        sendEgenAnsattløsning(hendelseId2, false)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = hendelseId2,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = hendelseId2
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = hendelseId2,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )

        // TODO: bør ikke koble seg på daoer i E2E
        assertTrue(oppgaveDao.finnOppgaver(false).any { it.fødselsnummer == FØDSELSNUMMER })

        val snapshot = vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER)
        assertNotNull(snapshot)
        val overstyringer = snapshot.arbeidsgivere.first().overstyringer
        assertEquals(1, overstyringer.size)
        assertEquals(1, overstyringer.first().overstyrteDager.size)
    }

    private fun assertSaksbehandlerOppgaveOpprettet(hendelseId: UUID) {
        val saksbehandlerOppgaver = oppgaveDao.finnOppgaver(false)
        assertEquals(
            1,
            saksbehandlerOppgaver.filter { it.oppgavereferanse == testRapid.inspektør.oppgaveId(hendelseId) }.size
        )
        assertTrue(saksbehandlerOppgaver.any { it.oppgavereferanse == testRapid.inspektør.oppgaveId(hendelseId) })
    }
}
