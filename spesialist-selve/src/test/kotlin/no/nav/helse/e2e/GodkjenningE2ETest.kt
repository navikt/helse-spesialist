package no.nav.helse.e2e

import AbstractE2ETest
import AbstractE2ETest.Kommandokjedetilstand.AVBRUTT
import AbstractE2ETest.Kommandokjedetilstand.FERDIG
import AbstractE2ETest.Kommandokjedetilstand.NY
import AbstractE2ETest.Kommandokjedetilstand.SUSPENDERT
import java.time.LocalDate
import java.util.UUID
import lagFødselsnummer
import no.nav.helse.GodkjenningsbehovTestdata
import no.nav.helse.januar
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.VergemålType.mindreaarig
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.VergemålType.voksen
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSystem
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GodkjenningE2ETest : AbstractE2ETest() {
    private companion object {
        private const val ENHET_UTLAND = "0393"
    }

    @Test
    fun `oppretter vedtak ved godkjenningsbehov`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterGodkjenningsbehov()
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()
        håndterArbeidsgiverinformasjonløsning()

        assertVedtaksperiodeEksisterer(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler godkjenner`() {
        fremTilSaksbehandleroppgave(regelverksvarsler = listOf("Brukeren har flere inntekter de siste tre måneder"))
        håndterSaksbehandlerløsning()

        assertSaksbehandleroppgave(oppgavestatus = AvventerSystem)
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = false)
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler avslår`() {
        fremTilSaksbehandleroppgave(regelverksvarsler = listOf("Brukeren har flere inntekter de siste tre måneder"))
        val begrunnelser = listOf("Mangler opptjening")
        val kommentar = "Vedkommende mangler opptjening"
        håndterSaksbehandlerløsning(godkjent = false, kommentar = kommentar, begrunnelser = begrunnelser)
        assertSaksbehandleroppgave(oppgavestatus = AvventerSystem)
        assertGodkjenningsbehovBesvart(godkjent = false, automatiskBehandlet = false)
        assertVedtaksperiodeAvvist("FØRSTEGANGSBEHANDLING", begrunnelser, kommentar)
    }

    @Test
    fun `oppretter ikke oppgave om bruker tilhører utlandsenhet`() {
        fremTilSaksbehandleroppgave(enhet = ENHET_UTLAND)

        assertVedtaksperiodeEksisterer(VEDTAKSPERIODE_ID)
        assertSaksbehandleroppgaveBleIkkeOpprettet()
        assertGodkjenningsbehovBesvart(false, automatiskBehandlet = true)
        assertKommandokjedetilstander(
            sisteGodkjenningsbehovId,
            NY, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, FERDIG
        )
    }

    @Test
    fun `avbryter suspendert kommando når godkjenningsbehov kommer inn på nytt`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterGodkjenningsbehov()
        val godkjenningsbehovId1 = sisteMeldingId
        håndterGodkjenningsbehov()
        assertKommandokjedetilstander(godkjenningsbehovId1, NY, SUSPENDERT, AVBRUTT)
    }

    @Test
    fun `ignorerer påminnet godkjenningsbehov dersom det eksisterer en aktiv oppgave`() {
        fremTilSaksbehandleroppgave()
        håndterGodkjenningsbehovUtenValidering()
        assertIngenEtterspurteBehov()
    }

    @Test
    fun `ignorerer påminnet godkjenningsbehov dersom vedtaket er automatisk godkjent`() {
        fremTilSaksbehandleroppgave(kanGodkjennesAutomatisk = true)
        håndterGodkjenningsbehovUtenValidering()
        assertIngenEtterspurteBehov()
    }

    @Test
    fun `legger ved ukjente organisasjonsnumre på behov for Arbeidsgiverinformasjon`() {
        val andreArbeidsgivere = listOf(testperson.orgnummer2)
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtaksperiodeNyUtbetaling()
        håndterGodkjenningsbehov(godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(orgnummereMedRelevanteArbeidsforhold = andreArbeidsgivere))
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()

        assertInnholdIBehov("Arbeidsgiverinformasjon") { behovNode ->
            val arbeidsgivere = behovNode["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map { it.asText() }
            assertEquals(andreArbeidsgivere, arbeidsgivere)
        }
    }

    @Test
    fun `skiller arbeidsgiverinformasjon- og personinfo-behov etter om det er et orgnr eller ikke`() {
        val arbeidsgiver2 = testperson.orgnummer2
        val arbeidsgiver3 = lagFødselsnummer()
        val andreArbeidsforhold = listOf(arbeidsgiver2, arbeidsgiver3)
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtaksperiodeNyUtbetaling()
        håndterGodkjenningsbehov(godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(orgnummereMedRelevanteArbeidsforhold = andreArbeidsforhold))
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()

        assertInnholdIBehov("HentPersoninfoV2") { behovNode ->
            val arbeidsgivere = behovNode["HentPersoninfoV2"]["ident"].map { it.asText() }
            assertEquals(listOf(arbeidsgiver3), arbeidsgivere)
        }

        assertInnholdIBehov("Arbeidsgiverinformasjon") { behovNode ->
            val arbeidsgivere = behovNode["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map { it.asText() }
            assertEquals(listOf(arbeidsgiver2), arbeidsgivere)
        }
    }

    @Test
    fun `tar inn arbeidsgiverinformasjon- og personinfo-behov samtidig`() {
        val arbeidsgiver2 = testperson.orgnummer2
        val arbeidsgiver3 = lagFødselsnummer()
        val andreArbeidsforhold = listOf(arbeidsgiver2, arbeidsgiver3)
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterGodkjenningsbehov(godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(orgnummereMedRelevanteArbeidsforhold = andreArbeidsforhold))
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()
        håndterArbeidsgiverinformasjonløsning() // henter først for ukjente arbeidsgivere (dvs. arbeidsgivere som ikke finnes i db)
        håndterArbeidsgiverinformasjonløsning() // henter deretter for arbeidsgiver som finnes i db - TODO: burde endres slik at vi henter for alle arbeidsgivere som mangler metadata samtidig
        assertKommandokjedetilstander(sisteGodkjenningsbehovId, NY, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT)
        assertSisteEtterspurteBehov("Arbeidsforhold")
    }

    @Test
    fun `legger til riktig felt for adressebeskyttelse i Personinfo`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterGodkjenningsbehov()
        håndterPersoninfoløsning(adressebeskyttelse = Adressebeskyttelse.StrengtFortrolig)
        assertAdressebeskyttelse(adressebeskyttelse = Adressebeskyttelse.StrengtFortrolig)
    }

    @Test
    fun `avbryter saksbehandling og avviser godkjenning på person med verge`() {
        fremTilVergemål()
        håndterVergemålløsning(vergemål = listOf(VergemålJson.Vergemål(voksen)))
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        assertGodkjenningsbehovBesvart(godkjent = false, automatiskBehandlet = true, "Vergemål")
    }

    @Test
    fun `avbryter ikke saksbehandling for person uten verge`() {
        fremTilVergemål()
        håndterVergemålløsning()
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `sendes til saksbehandler for godkjenning ved fullmakt`() {
        fremTilVergemål()
        håndterVergemålløsning(
            fullmakter = listOf(
                VergemålJson.Fullmakt(
                    områder = listOf(VergemålJson.Område.Syk),
                    gyldigTilOgMed = LocalDate.now(),
                    gyldigFraOgMed = LocalDate.now()
                )
            )
        )
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        assertSaksbehandleroppgave(VEDTAKSPERIODE_ID, AvventerSaksbehandler)
    }

    @Test
    fun `sendes til saksbehandler for godkjenning ved fremtidsfullmakt`() {
        fremTilVergemål()
        håndterVergemålløsning(fremtidsfullmakter = listOf(VergemålJson.Vergemål(voksen)))
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        assertSaksbehandleroppgave(VEDTAKSPERIODE_ID, AvventerSaksbehandler)
    }

    @Test
    fun `avbryter saksbehandling og avvise godkjenning pga vergemål`() {
        fremForbiUtbetalingsfilter()
        håndterEgenansattløsning(erEgenAnsatt = true)
        håndterVergemålløsning(vergemål = listOf(VergemålJson.Vergemål(voksen)))
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        assertKommandokjedetilstander(
            sisteGodkjenningsbehovId,
            NY, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, FERDIG
        )
        assertGodkjenningsbehovBesvart(godkjent = false, automatiskBehandlet = true, "Vergemål")

    }

    @Test
    fun `avviser ikke godkjenningsbehov når kanAvvises-flagget er false`() {
        automatiskGodkjent(11.januar, 31.januar)

        val revurdertUtbetaling = UUID.randomUUID()
        val kanAvvises = false

        håndterGodkjenningsbehov(
            harOppdatertMetainfo = true,
            godkjenningsbehovTestdata = GodkjenningsbehovTestdata(
                periodeFom = 11.januar,
                periodeTom = 31.januar,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                utbetalingId = revurdertUtbetaling,
                kanAvvises = kanAvvises,
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                organisasjonsnummer = ORGNR,
            )
        )

        håndterVergemålløsning(vergemål = listOf(VergemålJson.Vergemål(type = mindreaarig)))
        håndterÅpneOppgaverløsning()
        håndterInntektløsning()

        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }
}
