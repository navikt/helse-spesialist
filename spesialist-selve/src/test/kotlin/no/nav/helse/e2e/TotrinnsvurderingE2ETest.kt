package no.nav.helse.e2e

import AbstractE2ETest
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendOverstyrtArbeidsforhold
import no.nav.helse.Meldingssender.sendOverstyrtInntekt
import no.nav.helse.Meldingssender.sendOverstyrteDager
import no.nav.helse.Meldingssender.sendVedtaksperiodeEndret
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.ORGNR_GHOST
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.januar
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TotrinnsvurderingE2ETest : AbstractE2ETest() {

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av inntekt`() {
        settOppBruker()
        sendOverstyrtInntekt(
            månedligInntekt = 25000.0,
            fraMånedligInntekt = 25001.0,
            skjæringstidspunkt = 1.januar,
            forklaring = "vår egen forklaring",
            subsumsjon = null
        )
        val ekstern_hendelse_id = testRapid.inspektør.hendelser("overstyr_inntekt").first().let {
            UUID.fromString(it.path("@id").asText())
        }
        sendVedtaksperiodeEndret(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            forrigeTilstand = "FORRIGE_TILSTAND",
            gjeldendeTilstand = "GJELDENDE_TILSTAND",
            forårsaketAvId = ekstern_hendelse_id
        )
        val overstyrtType = overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(VEDTAKSPERIODE_ID)

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
        val ekstern_hendelse_id = testRapid.inspektør.hendelser("overstyr_arbeidsforhold").first().let {
            UUID.fromString(it.path("@id").asText())
        }
        sendVedtaksperiodeEndret(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            forrigeTilstand = "FORRIGE_TILSTAND",
            gjeldendeTilstand = "GJELDENDE_TILSTAND",
            forårsaketAvId = ekstern_hendelse_id
        )

        val overstyrtType = overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(VEDTAKSPERIODE_ID)

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
                    grad = null,
                    fraType = Dagtype.Sykedag,
                    fraGrad = 100
                )
            )
        )
        val ekstern_hendelse_id = testRapid.inspektør.hendelser("overstyr_tidslinje").first().let {
            UUID.fromString(it.path("@id").asText())
        }
        sendVedtaksperiodeEndret(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            forrigeTilstand = "FORRIGE_TILSTAND",
            gjeldendeTilstand = "GJELDENDE_TILSTAND",
            forårsaketAvId = ekstern_hendelse_id
        )

        val overstyrtType = overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(VEDTAKSPERIODE_ID)

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
