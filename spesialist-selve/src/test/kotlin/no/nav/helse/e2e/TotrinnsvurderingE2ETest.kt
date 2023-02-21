package no.nav.helse.e2e

import AbstractE2ETest
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendOverstyrTidslinje
import no.nav.helse.Meldingssender.sendOverstyringIgangsatt
import no.nav.helse.Meldingssender.sendOverstyrtArbeidsforhold
import no.nav.helse.Meldingssender.sendOverstyrtInntektOgRefusjon
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.ORGNR_GHOST
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.januar
import no.nav.helse.mediator.api.Arbeidsgiver
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.mediator.api.SubsumsjonDto
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TotrinnsvurderingE2ETest : AbstractE2ETest() {

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av inntekt og refusjon`() {
        settOppBruker()
        sendOverstyrtInntektOgRefusjon(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = LocalDate.now(),
            arbeidsgivere = listOf(
                Arbeidsgiver(
                    organisasjonsnummer = ORGNR,
                    månedligInntekt = 25000.0,
                    fraMånedligInntekt = 25001.0,
                    forklaring = "testbortforklaring",
                    subsumsjon = SubsumsjonDto("8-28", "LEDD_1", "BOKSTAV_A"),
                    refusjonsopplysninger = null,
                    fraRefusjonsopplysninger = null,
                    begrunnelse = "en begrunnelse")
            )
        )
        val ekstern_hendelse_id = testRapid.inspektør.hendelser("overstyr_inntekt_og_refusjon").first().let {
            UUID.fromString(it.path("@id").asText())
        }

        sendOverstyringIgangsatt(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            berørtePerioder = listOf(mapOf(
                "vedtaksperiodeId" to "$VEDTAKSPERIODE_ID"
            )),
            kilde = ekstern_hendelse_id
        )

        val overstyrtType = overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(VEDTAKSPERIODE_ID)

        val nyttGodkjenningsbehov = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
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
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
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

        sendOverstyringIgangsatt(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            berørtePerioder = listOf(mapOf(
                "vedtaksperiodeId" to "$VEDTAKSPERIODE_ID"
            )),
            kilde = ekstern_hendelse_id
        )

        val overstyrtType = overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(VEDTAKSPERIODE_ID)

        val nyttGodkjenningsbehov = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
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
        sendOverstyrTidslinje(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR,
            organisasjonsnummer = ORGNR,
            dager = listOf(
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
        sendOverstyringIgangsatt(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            berørtePerioder = listOf(mapOf(
                "vedtaksperiodeId" to "$VEDTAKSPERIODE_ID"
            )),
            kilde = ekstern_hendelse_id
        )

        val overstyrtType = overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(VEDTAKSPERIODE_ID)

        val nyttGodkjenningsbehov = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
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
