package no.nav.helse.e2e

import AbstractE2ETest
import AbstractE2ETest.Kommandokjedetilstand.AVBRUTT
import AbstractE2ETest.Kommandokjedetilstand.FERDIG
import AbstractE2ETest.Kommandokjedetilstand.NY
import AbstractE2ETest.Kommandokjedetilstand.SUSPENDERT
import kotliquery.queryOf
import kotliquery.sessionOf
import lagFødselsnummer
import no.nav.helse.GodkjenningsbehovTestdata
import no.nav.helse.januar
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.VergemålType.mindreaarig
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.VergemålType.voksen
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSystem
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class GodkjenningE2ETest : AbstractE2ETest() {
    private companion object {
        private const val ENHET_UTLAND = "0393"
    }

    @Test
    fun `oppretter vedtak ved godkjenningsbehov`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()
        håndterArbeidsgiverinformasjonløsning()

        assertVedtaksperiodeEksisterer(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler godkjenner`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(regelverksvarsler = listOf("Brukeren har flere inntekter de siste tre måneder"))
        håndterSaksbehandlerløsning()

        assertSaksbehandleroppgave(oppgavestatus = AvventerSystem)
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = false)
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler avslår`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(regelverksvarsler = listOf("Brukeren har flere inntekter de siste tre måneder"))
        val begrunnelser = listOf("Mangler opptjening")
        val kommentar = "Vedkommende mangler opptjening"
        håndterSaksbehandlerløsning(godkjent = false, kommentar = kommentar, begrunnelser = begrunnelser)
        assertSaksbehandleroppgave(oppgavestatus = AvventerSystem)
        assertGodkjenningsbehovBesvart(godkjent = false, automatiskBehandlet = false)
        assertVedtaksperiodeAvvist("FØRSTEGANGSBEHANDLING", begrunnelser, kommentar)
    }

    @Test
    fun `oppretter ikke oppgave om bruker tilhører utlandsenhet`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(enhet = ENHET_UTLAND)

        assertVedtaksperiodeEksisterer(VEDTAKSPERIODE_ID)
        assertSaksbehandleroppgaveBleIkkeOpprettet()
        assertGodkjenningsbehovBesvart(false, automatiskBehandlet = true)
        assertKommandokjedetilstander(
            sisteGodkjenningsbehovId,
            NY, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, FERDIG
        )
    }

    @Test
    fun `avbryter suspendert kommando når godkjenningsbehov kommer inn på nytt`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()
        val godkjenningsbehovId1 = sisteMeldingId
        håndterGodkjenningsbehov()
        assertKommandokjedetilstander(godkjenningsbehovId1, NY, SUSPENDERT, AVBRUTT)
    }

    @Test
    fun `ignorerer påminnet godkjenningsbehov dersom det eksisterer en aktiv oppgave`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterGodkjenningsbehovUtenValidering()
        assertIngenEtterspurteBehov()
    }

    @Test
    fun `ignorerer påminnet godkjenningsbehov dersom vedtaket er automatisk godkjent`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(kanGodkjennesAutomatisk = true)
        håndterGodkjenningsbehovUtenValidering()
        assertIngenEtterspurteBehov()
    }

    @Test
    fun `oppdaterer behandlingsinformasjon ved påminnet godkjenningsbehov`() {
        val spleisBehandlingId1 = UUID.randomUUID()
        val tags1 = listOf("tag 1", "tag 2")
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            kanGodkjennesAutomatisk = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(tags = tags1, spleisBehandlingId = spleisBehandlingId1)
        )
        assertBehandlingsinformasjon(VEDTAKSPERIODE_ID, tags1, spleisBehandlingId1)

        val spleisBehandlingId2 = UUID.randomUUID()
        val tags2 = listOf("tag 2", "tag 3")
        håndterGodkjenningsbehovUtenValidering(
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(tags = tags2, spleisBehandlingId = spleisBehandlingId2)
        )
        assertBehandlingsinformasjon(VEDTAKSPERIODE_ID, tags2, spleisBehandlingId2)
    }

    @Test
    fun `legger ved ukjente organisasjonsnumre på behov for Arbeidsgiverinformasjon`() {
        val andreArbeidsgivere = listOf(testperson.orgnummer2)
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
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
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
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
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
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
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()
        håndterPersoninfoløsning(adressebeskyttelse = Adressebeskyttelse.StrengtFortrolig)
        assertAdressebeskyttelse(adressebeskyttelse = Adressebeskyttelse.StrengtFortrolig)
    }

    @Test
    fun `avbryter saksbehandling og avviser godkjenning på person med verge`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilVergemål()
        håndterVergemålløsning(vergemål = listOf(VergemålJson.Vergemål(voksen)))
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        håndterAutomatiseringStoppetAvVeilederløsning()
        assertGodkjenningsbehovBesvart(godkjent = false, automatiskBehandlet = true, "Vergemål")
    }

    @Test
    fun `avbryter ikke saksbehandling for person uten verge`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilVergemål()
        håndterVergemålløsning()
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        håndterAutomatiseringStoppetAvVeilederløsning()
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `sendes til saksbehandler for godkjenning ved fullmakt`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilVergemål()
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
        håndterAutomatiseringStoppetAvVeilederløsning()
        assertSaksbehandleroppgave(VEDTAKSPERIODE_ID, AvventerSaksbehandler)
    }

    @Test
    fun `sendes til saksbehandler for godkjenning ved fremtidsfullmakt`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilVergemål()
        håndterVergemålløsning(fremtidsfullmakter = listOf(VergemålJson.Vergemål(voksen)))
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        håndterAutomatiseringStoppetAvVeilederløsning()
        assertSaksbehandleroppgave(VEDTAKSPERIODE_ID, AvventerSaksbehandler)
    }

    @Test
    fun `avbryter saksbehandling og avvise godkjenning pga vergemål`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovTilOgMedUtbetalingsfilter()
        håndterEgenansattløsning(erEgenAnsatt = true)
        håndterVergemålløsning(vergemål = listOf(VergemålJson.Vergemål(voksen)))
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        håndterAutomatiseringStoppetAvVeilederløsning()
        assertKommandokjedetilstander(
            sisteGodkjenningsbehovId,
            NY, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, FERDIG
        )
        assertGodkjenningsbehovBesvart(godkjent = false, automatiskBehandlet = true, "Vergemål")

    }

    @Test
    fun `avviser ikke godkjenningsbehov når kanAvvises-flagget er false`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistInnvilgerAutomatisk(11.januar, 31.januar)

        val revurdertUtbetaling = UUID.randomUUID()
        val kanAvvises = false

        spleisOppretterNyBehandling()
        håndterVedtaksperiodeNyUtbetaling(utbetalingId = revurdertUtbetaling)
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

    private fun assertBehandlingsinformasjon(
        vedtaksperiodeId: UUID,
        forventedeTags: List<String>,
        forventetSpleisBehandlingId: UUID
    ) {
        @Language("PostgreSQL")
        val query =
            "SELECT tags, spleis_behandling_id FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = :vedtaksperiodeId;"

        val (tags, spleisBehandlingId) = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, mapOf("vedtaksperiodeId" to vedtaksperiodeId))
                .map { it.array<String>("tags").toList() to it.uuid("spleis_behandling_id") }.asSingle
            )
        }!!

        assertEquals(forventedeTags, tags)
        assertEquals(forventetSpleisBehandlingId, spleisBehandlingId)
    }
}
