package no.nav.helse.e2e

import AbstractE2ETest
import java.time.LocalDate
import no.nav.helse.januar
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.overstyring.Dagtype
import no.nav.helse.overstyring.OverstyringDagDto
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
        assertTrue(overstyrtVedtaksperiodeDao.erVedtaksperiodeOverstyrt(VEDTAKSPERIODE_ID))
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
        assertTrue(overstyrtVedtaksperiodeDao.erVedtaksperiodeOverstyrt(VEDTAKSPERIODE_ID))
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
        assertTrue(overstyrtVedtaksperiodeDao.erVedtaksperiodeOverstyrt(VEDTAKSPERIODE_ID))
    }
}