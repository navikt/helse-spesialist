package no.nav.helse.e2e

import AbstractE2ETest
import java.time.LocalDate
import no.nav.helse.januar
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.overstyring.Dagtype
import no.nav.helse.overstyring.OverstyringDagDto
import no.nav.helse.overstyring.OverstyringType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TotrinnsvurderingE2ETest : AbstractE2ETest() {

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av inntekt`() {
        settOppBruker()
        sendOverstyrtInntekt(
            månedligInntekt = 25000.0,
            skjæringstidspunkt = 1.januar,
            forklaring = "vår egen forklaring"
        )
        val overstyrtType = overstyrtVedtaksperiodeDao.hentVedtaksperiodeOverstyrtTyper(VEDTAKSPERIODE_ID)

        val nyttGodkjenningsbehov = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            periodeFom = 1.januar,
            periodeTom = 31.januar,
            skjæringstidspunkt = 1.januar
        )
        klargjørForGodkjenning(nyttGodkjenningsbehov)
        val oppgaveId = oppgaveDao.finnOppgaveId(VEDTAKSPERIODE_ID)

        assertEquals(1, overstyrtType.size)
        assertEquals(OverstyringType.Inntekt, overstyrtType[0])
        assertTrue(oppgaveMediator.trengerTotrinnsvurdering(oppgaveId!!))
    }

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av arbeidsforhold`() {
        settOppBruker(orgnummereMedRelevanteArbeidsforhold = listOf(ORGNR_GHOST))
        sendOverstyrtArbeidsforhold(
            skjæringstidspunkt = 1.januar,
            overstyrteArbeidsforhold = listOf(
                OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt(
                    orgnummer = ORGNR_GHOST,
                    deaktivert = true,
                    begrunnelse = "begrunnelse",
                    forklaring = "forklaring"
                )
            )
        )
        val overstyrtType = overstyrtVedtaksperiodeDao.hentVedtaksperiodeOverstyrtTyper(VEDTAKSPERIODE_ID)

        val nyttGodkjenningsbehov = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            periodeFom = 1.januar,
            periodeTom = 31.januar,
            skjæringstidspunkt = 1.januar
        )
        klargjørForGodkjenning(nyttGodkjenningsbehov)
        val oppgaveId = oppgaveDao.finnOppgaveId(VEDTAKSPERIODE_ID)

        assertEquals(1, overstyrtType.size)
        assertEquals(OverstyringType.Arbeidsforhold, overstyrtType[0])
        assertTrue(oppgaveMediator.trengerTotrinnsvurdering(oppgaveId!!))
    }

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av tidslinje`() {
        settOppBruker()
        sendOverstyrteDager(
            listOf(
                OverstyringDagDto(
                    dato = LocalDate.of(2018, 1, 20),
                    type = Dagtype.Feriedag,
                    grad = null
                )
            )
        )
        val overstyrtType = overstyrtVedtaksperiodeDao.hentVedtaksperiodeOverstyrtTyper(VEDTAKSPERIODE_ID)

        val nyttGodkjenningsbehov = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            periodeFom = 1.januar,
            periodeTom = 31.januar,
            skjæringstidspunkt = 1.januar
        )
        klargjørForGodkjenning(nyttGodkjenningsbehov)
        val oppgaveId = oppgaveDao.finnOppgaveId(VEDTAKSPERIODE_ID)

        assertEquals(1, overstyrtType.size)
        assertEquals(OverstyringType.Dager, overstyrtType[0])
        assertTrue(oppgaveMediator.trengerTotrinnsvurdering(oppgaveId!!))
    }
}