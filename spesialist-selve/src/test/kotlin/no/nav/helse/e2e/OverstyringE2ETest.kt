package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import java.time.LocalDate
import no.nav.helse.Meldingssender.sendArbeidsforholdløsningOld
import no.nav.helse.Meldingssender.sendArbeidsgiverinformasjonløsningOld
import no.nav.helse.Meldingssender.sendDigitalKontaktinformasjonløsningOld
import no.nav.helse.Meldingssender.sendEgenAnsattløsningOld
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendOverstyrtArbeidsforhold
import no.nav.helse.Meldingssender.sendOverstyrtInntekt
import no.nav.helse.Meldingssender.sendOverstyrteDager
import no.nav.helse.Meldingssender.sendPersoninfoløsningComposite
import no.nav.helse.Meldingssender.sendRisikovurderingløsningOld
import no.nav.helse.Meldingssender.sendVergemålløsningOld
import no.nav.helse.Meldingssender.sendÅpneGosysOppgaverløsningOld
import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.Testdata.AKTØR
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
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.graphql.schema.Arbeidsforholdoverstyring
import no.nav.helse.spesialist.api.graphql.schema.Dagoverstyring
import no.nav.helse.spesialist.api.graphql.schema.Inntektoverstyring
import no.nav.helse.spesialist.api.graphql.schema.OppgaveForOversiktsvisning
import no.nav.helse.spesialist.api.graphql.schema.Person
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OverstyringE2ETest : AbstractE2ETest() {

    private fun List<OppgaveForOversiktsvisning>.ingenOppgaveMedId(id: String) = none { it.id == id }
    private fun assertIngenOppgaver(id: String) {
        oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_UTEN_TILGANGER).ingenOppgaveMedId(id)
    }

    @Test
    fun `saksbehandler overstyrer sykdomstidslinje`() {
        val originaltGodkjenningsbehov = settOppBruker()

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

        assertTrue(overstyringApiDao.finnOverstyringerAvTidslinjer(FØDSELSNUMMER, ORGNR).isNotEmpty())
        val originalOppgaveId = testRapid.inspektør.oppgaveId(originaltGodkjenningsbehov)
        assertIngenOppgaver(originalOppgaveId)

        val nyttGodkjenningsbehov = sendGodkjenningsbehov(
            AKTØR,
            FØDSELSNUMMER,
            ORGNR,
            VEDTAKSPERIODE_ID,
            UTBETALING_ID,
            LocalDate.of(2018, 1, 1),
            LocalDate.of(2018, 1, 31)
        )

        klargjørForGodkjenning(nyttGodkjenningsbehov)

        val oppgave =
            oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_UTEN_TILGANGER).find { it.fodselsnummer == FØDSELSNUMMER }
        assertEquals(SAKSBEHANDLER_EPOST, oppgave!!.tildeling?.epost)
    }

    @Test
    fun `saksbehandler overstyrer inntekt`() {
        val godkjenningsbehovId = settOppBruker()
        val hendelseId = sendOverstyrtInntekt(
            månedligInntekt = 25000.0,
            fraMånedligInntekt = 25001.0,
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
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            periodeFom = 1.januar,
            periodeTom = 31.januar,
            skjæringstidspunkt = 1.januar
        )

        klargjørForGodkjenning(nyttGodkjenningsbehov)

        val oppgave = requireNotNull(oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_UTEN_TILGANGER)
            .find { it.fodselsnummer == FØDSELSNUMMER })
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
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            periodeFom = 1.januar,
            periodeTom = 31.januar,
            skjæringstidspunkt = 1.januar
        )

        klargjørForGodkjenning(nyttGodkjenningsbehov)

        val oppgave = requireNotNull(oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_UTEN_TILGANGER)
            .find { it.fodselsnummer == FØDSELSNUMMER })
        assertEquals(SAKSBEHANDLER_EPOST, oppgave.tildeling?.epost)
    }

    @Test
    fun `legger ved overstyringer i speil snapshot`() {
        val hendelseId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31)
        )
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_MED_WARNINGS
        every { dataFetchingEnvironment.graphQlContext.get<String>("saksbehandlerNavn") } returns "saksbehandler"
        every { dataFetchingEnvironment.graphQlContext.get<SaksbehandlerTilganger>("tilganger") } returns saksbehandlerTilganger

        sendPersoninfoløsningComposite(hendelseId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = hendelseId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            navn = "En Arbeidsgiver",
            bransjer = listOf("Bransje1", "Bransje2")
        )
        sendArbeidsforholdløsningOld(
            hendelseId = hendelseId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(hendelseId, false)
        sendVergemålløsningOld(
            godkjenningsmeldingId = hendelseId
        )
        sendDigitalKontaktinformasjonløsningOld(
            godkjenningsmeldingId = hendelseId,
            erDigital = true
        )
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
        sendOverstyrtInntekt(
            orgnr = ORGNR,
            månedligInntekt = 15000.0,
            fraMånedligInntekt = 15001.0,
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
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31)
        )
        sendEgenAnsattløsningOld(hendelseId2, false)
        sendVergemålløsningOld(
            godkjenningsmeldingId = hendelseId2
        )
        sendDigitalKontaktinformasjonløsningOld(
            godkjenningsmeldingId = hendelseId2,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = hendelseId2
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = hendelseId2,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )

        // TODO: bør ikke koble seg på daoer i E2E
        assertTrue(
            oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_UTEN_TILGANGER).any { it.fodselsnummer == FØDSELSNUMMER })

        val snapshot: Person = personQuery.person(FØDSELSNUMMER, null, dataFetchingEnvironment).data!!

        assertNotNull(snapshot)
        val overstyringer = snapshot.arbeidsgivere().first().overstyringer()
        assertEquals(3, overstyringer.size)
        assertEquals(1, (overstyringer.first() as Dagoverstyring).dager.size)
        assertEquals(15000.0, (overstyringer[1] as Inntektoverstyring).inntekt.manedligInntekt)
        assertEquals(true, (overstyringer[2] as Arbeidsforholdoverstyring).deaktivert)
    }
}
