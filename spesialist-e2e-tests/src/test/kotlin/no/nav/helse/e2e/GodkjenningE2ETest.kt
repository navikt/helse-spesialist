package no.nav.helse.e2e

import no.nav.helse.GodkjenningsbehovTestdata
import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.e2e.AbstractE2ETest.Kommandokjedetilstand.AVBRUTT
import no.nav.helse.e2e.AbstractE2ETest.Kommandokjedetilstand.FERDIG
import no.nav.helse.e2e.AbstractE2ETest.Kommandokjedetilstand.NY
import no.nav.helse.e2e.AbstractE2ETest.Kommandokjedetilstand.SUSPENDERT
import no.nav.helse.modell.person.Adressebeskyttelse.StrengtFortrolig
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSystem
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk.VergemålJson
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk.VergemålJson.VergemålType.mindreaarig
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk.VergemålJson.VergemålType.voksen
import no.nav.helse.util.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class GodkjenningE2ETest : AbstractE2ETest() {
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
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(regelverksvarsler = listOf("RV_SV_4"))
        håndterSaksbehandlerløsning()

        assertSaksbehandleroppgave(oppgavestatus = AvventerSystem)
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = false)
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler avslår`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(regelverksvarsler = listOf("RV_SV_4"))
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
            NY,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            FERDIG,
        )
    }

    @Test
    fun `avbryter suspendert kommando når godkjenningsbehov kommer inn på nytt`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()
        val godkjenningsbehovId1 = sisteGodkjenningsbehovId
        håndterGodkjenningsbehov()
        assertKommandokjedetilstander(godkjenningsbehovId1, NY, SUSPENDERT, SUSPENDERT, AVBRUTT)
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
        spleisOppretterNyBehandling(spleisBehandlingId = spleisBehandlingId1)
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            kanGodkjennesAutomatisk = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(tags = tags1, spleisBehandlingId = spleisBehandlingId1),
        )
        assertBehandlingsinformasjon(VEDTAKSPERIODE_ID, tags1, spleisBehandlingId1)

        val tags2 = listOf("tag 2", "tag 3")
        håndterGodkjenningsbehovUtenValidering(
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(tags = tags2, spleisBehandlingId = spleisBehandlingId1),
        )
        assertBehandlingsinformasjon(VEDTAKSPERIODE_ID, tags2, spleisBehandlingId1)
    }

    @Test
    fun `legger ved ukjente organisasjonsnumre på behov for Arbeidsgiverinformasjon`() {
        val andreArbeidsgivere = listOf(testperson.orgnummer2)
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterVedtaksperiodeNyUtbetaling()
        håndterGodkjenningsbehov(
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(orgnummereMedRelevanteArbeidsforhold = andreArbeidsgivere),
        )
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()

        assertInnholdIBehov("Arbeidsgiverinformasjon") { behovNode ->
            val arbeidsgivere = behovNode["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map { it.asText() }
            assertEquals((andreArbeidsgivere + testperson.orgnummer).toSet(), arbeidsgivere.toSet())
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
        håndterGodkjenningsbehov(
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(orgnummereMedRelevanteArbeidsforhold = andreArbeidsforhold),
        )
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()

        assertInnholdIBehov("HentPersoninfoV2") { behovNode ->
            val arbeidsgivere = behovNode["HentPersoninfoV2"]["ident"].map { it.asText() }
            assertEquals(listOf(arbeidsgiver3), arbeidsgivere)
        }

        assertInnholdIBehov("Arbeidsgiverinformasjon") { behovNode ->
            val arbeidsgivere = behovNode["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map { it.asText() }
            assertEquals(setOf(arbeidsgiver2, testperson.orgnummer), arbeidsgivere.toSet())
        }
    }

    @Test
    fun `tar inn arbeidsgiverinformasjon- og personinfo-behov samtidig`() {
        val arbeidsgiver2 = testperson.orgnummer2
        val arbeidsgiver3 = lagFødselsnummer()
        val andreArbeidsforhold = listOf(arbeidsgiver2, arbeidsgiver3)
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov(
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(orgnummereMedRelevanteArbeidsforhold = andreArbeidsforhold),
        )
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()
        håndterArbeidsgiverinformasjonløsning()
        assertKommandokjedetilstander(sisteGodkjenningsbehovId, NY, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT)
        assertSisteEtterspurteBehov("Arbeidsforhold")
    }

    @Test
    fun `legger til riktig felt for adressebeskyttelse i Personinfo`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()
        håndterPersoninfoløsning(adressebeskyttelse = StrengtFortrolig)
        assertAdressebeskyttelse(adressebeskyttelse = StrengtFortrolig)
    }

    @Test
    fun `avbryter saksbehandling og avviser godkjenning på person med verge`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilVergemål()
        håndterVergemålOgFullmaktløsning(vergemål = listOf(VergemålJson.Vergemål(VergemålJson.VergemålType.voksen)))
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        assertGodkjenningsbehovBesvart(godkjent = false, automatiskBehandlet = true, "Vergemål")
    }

    @Test
    fun `avbryter ikke saksbehandling for person uten verge`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilVergemål()
        håndterVergemålOgFullmaktløsning()
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `avbryter ikke saksbehandling ved fullmakt eller fremtidsfullmakt`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilVergemål()
        håndterVergemålOgFullmaktløsning(
            fullmakter =
                listOf(
                    VergemålJson.Fullmakt(
                        områder = listOf(VergemålJson.Område.Syk),
                        gyldigTilOgMed = LocalDate.now(),
                        gyldigFraOgMed = LocalDate.now(),
                    ),
                ),
            fremtidsfullmakter = listOf(VergemålJson.Vergemål(voksen))
        )
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `avbryter saksbehandling og avvise godkjenning pga vergemål`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilEgenAnsatt()
        håndterEgenansattløsning(erEgenAnsatt = true)
        håndterVergemålOgFullmaktløsning(vergemål = listOf(VergemålJson.Vergemål(voksen)))
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        assertKommandokjedetilstander(
            sisteGodkjenningsbehovId,
            NY,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            FERDIG,
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
        val spleisBehandlingId = UUID.randomUUID()

        spleisOppretterNyBehandling(spleisBehandlingId = spleisBehandlingId)
        håndterVedtaksperiodeNyUtbetaling(utbetalingId = revurdertUtbetaling)
        håndterGodkjenningsbehov(
            harOppdatertMetainfo = true,
            godkjenningsbehovTestdata =
                GodkjenningsbehovTestdata(
                    periodeFom = 11.januar,
                    periodeTom = 31.januar,
                    vedtaksperiodeId = VEDTAKSPERIODE_ID,
                    utbetalingId = revurdertUtbetaling,
                    kanAvvises = kanAvvises,
                    fødselsnummer = FØDSELSNUMMER,
                    aktørId = AKTØR,
                    organisasjonsnummer = ORGNR,
                    spleisBehandlingId = spleisBehandlingId,
                ),
        )

        håndterVergemålOgFullmaktløsning(vergemål = listOf(VergemålJson.Vergemål(type = mindreaarig)))
        håndterÅpneOppgaverløsning()
        håndterInntektløsning()

        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `flytt eventuelle aktive avviksvarsler til periode som nå er til godkjenning`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val behandlingId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val behandlingId2 = UUID.randomUUID()

        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling(vedtaksperiodeId = vedtaksperiodeId1, fom = 1.januar, tom = 15.januar, spleisBehandlingId = behandlingId1) // AUU
        spleisOppretterNyBehandling(vedtaksperiodeId = vedtaksperiodeId2, fom = 1.januar, tom = 31.januar, spleisBehandlingId = behandlingId2) // Vanlig periode
        håndterAktivitetsloggNyAktivitet(vedtaksperiodeId = vedtaksperiodeId2, varselkoder = listOf("RV_IV_2")) // avviksvarsel

        // simulerer at første periode omgjøres
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            kanGodkjennesAutomatisk = true,
            godkjenningsbehovTestdata = GodkjenningsbehovTestdata(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                organisasjonsnummer = ORGNR,
                vedtaksperiodeId = vedtaksperiodeId1,
                utbetalingId = UTBETALING_ID,
                spleisBehandlingId = behandlingId1,
                skjæringstidspunkt = 1.januar
            ),
        )

        assertVarsler(vedtaksperiodeId1, 1)
        assertVarsel(vedtaksperiodeId1, "RV_IV_2")
        assertVarsler(vedtaksperiodeId2, 0)
    }

    @Test
    fun `ikke flytt vurdert avviksvarsel til annen (tidligere) periode som er til godkjenning`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val behandlingId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val behandlingId2 = UUID.randomUUID()

        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling(vedtaksperiodeId = vedtaksperiodeId1, fom = 1.januar, tom = 15.januar, spleisBehandlingId = behandlingId1) // AUU
        spleisOppretterNyBehandling(vedtaksperiodeId = vedtaksperiodeId2, fom = 1.januar, tom = 31.januar, spleisBehandlingId = behandlingId2) // Vanlig periode
        håndterAktivitetsloggNyAktivitet(vedtaksperiodeId = vedtaksperiodeId2, varselkoder = listOf("RV_IV_2")) // avviksvarsel

        saksbehandlerVurdererVarsel(vedtaksperiodeId2, "RV_IV_2")

        // simulerer at første periode omgjøres
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            kanGodkjennesAutomatisk = true,
            godkjenningsbehovTestdata = GodkjenningsbehovTestdata(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                organisasjonsnummer = ORGNR,
                vedtaksperiodeId = vedtaksperiodeId1,
                utbetalingId = UTBETALING_ID,
                spleisBehandlingId = behandlingId1,
                skjæringstidspunkt = 1.januar
            ),
        )

        assertVarsler(vedtaksperiodeId1, 0)
        assertVarsel(vedtaksperiodeId2, "RV_IV_2")
        assertVarsler(vedtaksperiodeId2, 1)
    }

    @Test
    fun `oppdaterer oppgavens peker til godkjenningsbehov ved mottak av nytt godkjenningsbehov`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        val gammelTag = "GAMMEL_KJEDELIG_TAG"
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(tags = listOf(gammelTag))
        )
        val oppgaveId = inspektør.oppgaveId()
        val godkjenningsbehovData = finnGodkjenningsbehovJson(oppgaveId).let { it["Godkjenning"] }
        check(godkjenningsbehovData["tags"].first().asText() == gammelTag)

        val nyTag = "NY_OG_BANEBRYTENDE_TAG"
        håndterGodkjenningsbehovUtenValidering(
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(tags = listOf(nyTag))
        )
        val oppdaterteGodkjenningsbehovData = finnGodkjenningsbehovJson(oppgaveId).let { it["Godkjenning"] }
        assertEquals(setOf(nyTag), oppdaterteGodkjenningsbehovData["tags"].map { it.asText() }.toSet())
    }

    private fun finnGodkjenningsbehovJson(oppgaveId: Long) = dbQuery.single(
        """
        select h.data from hendelse h
        join oppgave o on h.id = o.hendelse_id_godkjenningsbehov
        where o.id = :oppgaveId
        """.trimIndent(),
        "oppgaveId" to oppgaveId
    ) { it.string("data") }.let { objectMapper.readTree(it) }

    private fun assertBehandlingsinformasjon(
        vedtaksperiodeId: UUID,
        forventedeTags: List<String>,
        forventetSpleisBehandlingId: UUID,
    ) {
        val (tags, spleisBehandlingId) = dbQuery.single(
            "SELECT tags, spleis_behandling_id FROM behandling WHERE vedtaksperiode_id = :vedtaksperiodeId",
            "vedtaksperiodeId" to vedtaksperiodeId
        ) { it.array<String>("tags").toList() to it.uuid("spleis_behandling_id") }

        assertEquals(forventedeTags, tags)
        assertEquals(forventetSpleisBehandlingId, spleisBehandlingId)
    }
}
