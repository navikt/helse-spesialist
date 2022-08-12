package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import java.time.LocalDate
import no.nav.helse.Meldingssender.sendArbeidsforholdløsning
import no.nav.helse.Meldingssender.sendArbeidsgiverinformasjonløsning
import no.nav.helse.Meldingssender.sendDigitalKontaktinformasjonløsning
import no.nav.helse.Meldingssender.sendEgenAnsattløsning
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendOverstyrtArbeidsforhold
import no.nav.helse.Meldingssender.sendOverstyrtInntekt
import no.nav.helse.Meldingssender.sendOverstyrteDager
import no.nav.helse.Meldingssender.sendPersoninfoløsning
import no.nav.helse.Meldingssender.sendRisikovurderingløsning
import no.nav.helse.Meldingssender.sendVergemålløsning
import no.nav.helse.Meldingssender.sendÅpneGosysOppgaverløsning
import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.ORGNR_GHOST
import no.nav.helse.Testdata.SAKSBEHANDLERTILGANGER_UTEN_TILGANGER
import no.nav.helse.Testdata.SAKSBEHANDLER_EPOST
import no.nav.helse.Testdata.SNAPSHOT_MED_WARNINGS
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.januar
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.mediator.api.graphql.schema.Arbeidsforholdoverstyring
import no.nav.helse.mediator.api.graphql.schema.Dagoverstyring
import no.nav.helse.mediator.api.graphql.schema.Inntektoverstyring
import no.nav.helse.mediator.api.graphql.schema.Person
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.oppgave.OppgaveForOversiktsvisningDto
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OverstyringE2ETest : AbstractE2ETest() {

    private fun List<OppgaveForOversiktsvisningDto>.ingenOppgaveMedId(id: String) = none { it.oppgavereferanse == id }
    private fun assertIngenOppgaver(id: String) {
        oppgaveDao.finnOppgaver(SAKSBEHANDLERTILGANGER_UTEN_TILGANGER).ingenOppgaveMedId(id)
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

        val oppgave =
            oppgaveDao.finnOppgaver(SAKSBEHANDLERTILGANGER_UTEN_TILGANGER).find { it.fødselsnummer == FØDSELSNUMMER }
        assertEquals(SAKSBEHANDLER_EPOST, oppgave!!.tildeling?.epost)
    }

    @Test
    fun `saksbehandler overstyrer inntekt`() {
        val godkjenningsbehovId = settOppBruker()
        val hendelseId = sendOverstyrtInntekt(
            månedligInntekt = 25000.0,
            skjæringstidspunkt = 1.januar,
            forklaring = "vår egen forklaring",
            subsumsjon = null
        )

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
        assertEquals("vår egen forklaring", overstyringer.first().forklaring)

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

        val oppgave = requireNotNull(oppgaveDao.finnOppgaver(SAKSBEHANDLERTILGANGER_UTEN_TILGANGER)
            .find { it.fødselsnummer == FØDSELSNUMMER })
        assertEquals(SAKSBEHANDLER_EPOST, oppgave.tildeling?.epost)
    }

    @Test
    fun `saksbehandler overstyrer arbeidsforhold`() {
        val godkjenningsbehovId = settOppBruker(orgnummereMedRelevanteArbeidsforhold = listOf(ORGNR_GHOST))
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

        val overstyringer = overstyringApiDao.finnOverstyringerAvArbeidsforhold(
            fødselsnummer = FØDSELSNUMMER,
            orgnummer = ORGNR_GHOST
        )
        assertEquals(1, overstyringer.size)
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

        val oppgave = requireNotNull(oppgaveDao.finnOppgaver(SAKSBEHANDLERTILGANGER_UTEN_TILGANGER)
            .find { it.fødselsnummer == FØDSELSNUMMER })
        assertEquals(SAKSBEHANDLER_EPOST, oppgave.tildeling?.epost)
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
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_MED_WARNINGS
        every { dataFetchingEnvironment.graphQlContext.get<String>("saksbehandlerNavn") } returns "saksbehandler"
        every { dataFetchingEnvironment.graphQlContext.get<SaksbehandlerTilganger>("tilganger") } returns saksbehandlerTilganger

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
        sendVergemålløsning(
            godkjenningsmeldingId = hendelseId
        )
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
        sendOverstyrtInntekt(
            orgnr = ORGNR,
            månedligInntekt = 15000.0,
            skjæringstidspunkt = LocalDate.now(),
            forklaring = "forklaring",
            subsumsjon = null
        )
        sendOverstyrtArbeidsforhold(
            skjæringstidspunkt = LocalDate.of(2018, 1, 1),
            overstyrteArbeidsforhold = listOf(
                OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt(
                    orgnummer = ORGNR,
                    deaktivert = true,
                    begrunnelse = "begrunnelse",
                    forklaring = "forklaring"
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
        sendVergemålløsning(
            godkjenningsmeldingId = hendelseId2
        )
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
        assertTrue(
            oppgaveDao.finnOppgaver(SAKSBEHANDLERTILGANGER_UTEN_TILGANGER).any { it.fødselsnummer == FØDSELSNUMMER })

        val snapshot: Person = personQuery.person(FØDSELSNUMMER, null, dataFetchingEnvironment).data!!

        assertNotNull(snapshot)
        val overstyringer = snapshot.arbeidsgivere().first().overstyringer()
        assertEquals(3, overstyringer.size)
        assertEquals(1, (overstyringer.first() as Dagoverstyring).dager.size)
        assertEquals(15000.0, (overstyringer[1] as Inntektoverstyring).inntekt.manedligInntekt)
        assertEquals(true, (overstyringer[2] as Arbeidsforholdoverstyring).deaktivert)
    }
}
