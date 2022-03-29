package no.nav.helse.e2e

import AbstractE2ETest
import com.fasterxml.jackson.databind.JsonNode
import io.mockk.every
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.graphQLSnapshot
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Vergemål
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.VergemålType.voksen
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.oppgave.Oppgavestatus.AvventerSystem
import no.nav.helse.oppgave.Oppgavestatus.Ferdigstilt
import no.nav.helse.person.Adressebeskyttelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class GodkjenningE2ETest : AbstractE2ETest() {
    private companion object {
        private const val ENHET_UTLAND = "2101"
        private const val SAKSBEHANDLERIDENT = "Z999999"
        private const val AUTOMATISK_BEHANDLET = "Automatisk behandlet"
        private const val SAKSBEHANDLEREPOST = "saksbehandler@nav.no"
        private val SAKSBEHANDLEROID = UUID.randomUUID()
        private val ADRESSEBESKYTTELSE = Adressebeskyttelse.StrengtFortrolig
    }

    private val OPPGAVEID get() = testRapid.inspektør.oppgaveId()

    @Test
    fun `ignorerer endringer på ukjente vedtaksperioder`() {
        val hendelseId = sendVedtaksperiodeEndret(ORGNR, VEDTAKSPERIODE_ID)
        assertIkkeHendelse(hendelseId)
        assertIkkeVedtak(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `oppretter ikke vedtak ved godkjenningsbehov uten nødvendig informasjon`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        assertHendelse(godkjenningsmeldingId)
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT")
        assertBehov("HentPersoninfoV2", "HentEnhet", "HentInfotrygdutbetalinger")
        assertIkkeVedtak(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `ignorerer behov uten tilhørende command`() {
        val hendelseId = UUID.randomUUID()
        val contextId = UUID.randomUUID()
        sendPersoninfoløsning(hendelseId, ORGNR, VEDTAKSPERIODE_ID, contextId)
        assertIkkeHendelse(hendelseId)
    }

    @Test
    fun `oppretter vedtak ved godkjenningsbehov`() {
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_UTEN_WARNINGS
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(godkjenningsmeldingId, false)
        sendVergemålløsning(godkjenningsmeldingId)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        assertSnapshot(SNAPSHOTV1_UTEN_WARNINGS, VEDTAKSPERIODE_ID)
        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "FERDIG"
        )
        assertVedtak(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler godkjenner`() {
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_MED_WARNINGS //Legger på warning for at saken ikke skal automatiseres
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsning(godkjenningsmeldingId)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val løsningId =
            sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        sendUtbetalingEndret("UTBETALING", UTBETALT, ORGNR, "EN_FAGSYSTEMID", utbetalingId = UTBETALING_ID)
        assertSnapshot(SNAPSHOTV1_MED_WARNINGS, VEDTAKSPERIODE_ID)
        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "FERDIG"
        )
        assertTilstand(løsningId, "NY", "FERDIG")
        assertOppgavestatuser(0, AvventerSaksbehandler, AvventerSystem, Ferdigstilt)
        assertGodkjenningsbehovløsning(true, SAKSBEHANDLERIDENT)
        assertNotNull(testRapid.inspektør.hendelser("vedtaksperiode_godkjent").firstOrNull())
    }

    @Test
    fun `behovene spores tilbake`() {
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_MED_WARNINGS //Legger på warning for at saken ikke skal automatiseres
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        val personinfomeldingId = sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        val arbeidsgiverløsningId = sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val arbeidsforholdmeldingId = sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val egenansattMeldingId = sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        val vergemålløsningId = sendVergemålløsning(godkjenningsmeldingId)
        val dkifløsningId = sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        val åpnegosysoppgaverløsningId = sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        val risikoløsningId = sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val løsningId = sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)

        testRapid.inspektør.also { inspektør ->
            assertEquals(godkjenningsmeldingId.toString(), inspektør.field(0, "@forårsaket_av").path("id").asText())
            assertEquals(personinfomeldingId.toString(), inspektør.field(1, "@forårsaket_av").path("id").asText())
            assertEquals(arbeidsgiverløsningId.toString(), inspektør.field(2, "@forårsaket_av").path("id").asText())
            assertEquals(arbeidsforholdmeldingId.toString(), inspektør.field(3, "@forårsaket_av").path("id").asText())
            assertEquals(egenansattMeldingId.toString(), inspektør.field(4, "@forårsaket_av").path("id").asText())
            assertEquals(vergemålløsningId.toString(), inspektør.field(5, "@forårsaket_av").path("id").asText())
            assertEquals(dkifløsningId.toString(), inspektør.field(6, "@forårsaket_av").path("id").asText())
            assertEquals(åpnegosysoppgaverløsningId.toString(), inspektør.field(7, "@forårsaket_av").path("id").asText())
            assertEquals(risikoløsningId.toString(), inspektør.field(8, "@forårsaket_av").path("id").asText())
            assertEquals(godkjenningsmeldingId.toString(), inspektør.field(9, "@forårsaket_av").path("id").asText())
            assertEquals(godkjenningsmeldingId.toString(), inspektør.field(10, "@forårsaket_av").path("id").asText())
            assertEquals(løsningId.toString(), inspektør.field(11, "@forårsaket_av").path("id").asText())
            assertNotEquals(godkjenningsmeldingId.toString(), inspektør.field(11, "@id").asText())

            0.until(inspektør.size).forEach {
                println(inspektør.message(it))
            }
        }
    }

    @Test
    fun `slår sammen warnings fra spleis og spesialist i utgående event`() {
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_MED_WARNINGS
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsning(godkjenningsmeldingId)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            antall = 1,
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)

        val vedtaksperiodeGodkjentEvent = testRapid.inspektør.hendelser("vedtaksperiode_godkjent").firstOrNull()
        assertNotNull(vedtaksperiodeGodkjentEvent)
        vedtaksperiodeGodkjentEvent!!
        assertEquals(FØDSELSNUMMER, vedtaksperiodeGodkjentEvent["fødselsnummer"].asText())
        assertTrue(vedtaksperiodeGodkjentEvent.hasNonNull("@opprettet"))
        assertEquals(2, vedtaksperiodeGodkjentEvent["warnings"].size())
        assertEquals(WarningKilde.Spleis.name, vedtaksperiodeGodkjentEvent["warnings"][0]["kilde"].asText())
        assertEquals(WarningKilde.Spesialist.name, vedtaksperiodeGodkjentEvent["warnings"][1]["kilde"].asText())
        assertFalse(vedtaksperiodeGodkjentEvent["automatiskBehandling"].asBoolean())
        assertVedtaksperiodeGodkjentEvent(vedtaksperiodeGodkjentEvent)
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler avslår`() {
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_MED_WARNINGS //Legger på warning for at saken ikke skal automatiseres
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsning(godkjenningsmeldingId)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val begrunnelser = listOf("Fortjener ikke penger", "Skulker sannsynligvis")
        val kommentar = "Jeg har tatt meg litt frihet i vurderingen"
        val løsningId =
            sendSaksbehandlerløsning(
                OPPGAVEID,
                SAKSBEHANDLERIDENT,
                SAKSBEHANDLEREPOST,
                SAKSBEHANDLEROID,
                false,
                begrunnelser,
                kommentar
            )
        sendUtbetalingEndret("UTBETALING", UTBETALT, ORGNR, "EN_FAGSYSTEMID", utbetalingId = UTBETALING_ID)
        assertSnapshot(SNAPSHOTV1_MED_WARNINGS, VEDTAKSPERIODE_ID)
        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "FERDIG"
        )
        assertTilstand(løsningId, "NY", "FERDIG")
        assertOppgavestatuser(0, AvventerSaksbehandler, AvventerSystem, Ferdigstilt)
        assertGodkjenningsbehovløsning(false, SAKSBEHANDLERIDENT)
        assertVedtaksperiodeAvvist("FØRSTEGANGSBEHANDLING", begrunnelser, kommentar)
    }

    @Test
    fun `endringer på kjente vedtaksperioder`() {
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returnsMany listOf(
            SNAPSHOTV1_UTEN_WARNINGS,
            SNAPSHOTV1_UTEN_WARNINGS
        )
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)

        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val endringsmeldingId = sendVedtaksperiodeEndret(ORGNR, VEDTAKSPERIODE_ID)
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT")
        assertTilstand(endringsmeldingId, "NY", "FERDIG")
        assertSnapshot(SNAPSHOTV1_UTEN_WARNINGS, VEDTAKSPERIODE_ID)
        verify(exactly = 2) { restClient.hentSpeilSnapshot(FØDSELSNUMMER) }
    }

    @Test
    fun `vedtaksperiode forkastet`() {
        val hendelseId = sendVedtaksperiodeForkastet(ORGNR, VEDTAKSPERIODE_ID)
        assertIkkeHendelse(hendelseId)
        assertIkkeVedtak(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `vedtaksperiode forkastet når det finnes en hendelse`() {
        val hendelseId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendVedtaksperiodeForkastet(ORGNR, VEDTAKSPERIODE_ID)
        assertHendelse(hendelseId)
        assertIkkeVedtak(VEDTAKSPERIODE_ID)
        assertTilstand(hendelseId, "NY", "SUSPENDERT", "AVBRUTT")
    }

    @Test
    fun `gjør ingen ting om man får tilbake løsning på en avbrutt command context`() {
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_UTEN_WARNINGS
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val hendelseId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(hendelseId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = hendelseId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            navn = "En Arbeidsgiver",
            bransjer = listOf("En eller flere bransjer")
        )
        sendArbeidsforholdløsning(
            hendelseId = hendelseId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendVedtaksperiodeForkastet(ORGNR, VEDTAKSPERIODE_ID)

        assertTilstand(hendelseId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "AVBRUTT")
        sendPersoninfoløsning(hendelseId, ORGNR, VEDTAKSPERIODE_ID)
        assertTilstand(hendelseId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "AVBRUTT")
    }

    @Test
    fun `oppretter ikke oppgave om bruker er egen ansatt`() {
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_UTEN_WARNINGS
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            navn = "En Arbeidsgiver",
            bransjer = listOf("En eller flere bransjer")
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(godkjenningsmeldingId, true)
        sendVergemålløsning(godkjenningsmeldingId)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        assertSnapshot(SNAPSHOTV1_UTEN_WARNINGS, VEDTAKSPERIODE_ID)
        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "FERDIG"
        )
        assertVedtak(VEDTAKSPERIODE_ID)
        assertIngenOppgave()
        assertGodkjenningsbehovløsning(false, AUTOMATISK_BEHANDLET)
    }

    @Test
    fun `oppretter ikke oppgave om bruker tilhører utlandsenhet`() {
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_UTEN_WARNINGS
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID, enhet = ENHET_UTLAND)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            navn = "En Arbeidsgiver",
            bransjer = listOf("En eller flere bransjer")
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(godkjenningsmeldingId, false)
        sendVergemålløsning(godkjenningsmeldingId)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        assertSnapshot(SNAPSHOTV1_UTEN_WARNINGS, VEDTAKSPERIODE_ID)
        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "FERDIG"
        )
        assertVedtak(VEDTAKSPERIODE_ID)
        assertIngenOppgave()
        assertGodkjenningsbehovløsning(false, AUTOMATISK_BEHANDLET)
    }

    @Test
    fun `vanlig arbeidsforhold`() {
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_UTEN_WARNINGS
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(godkjenningsmeldingId, false)
        sendVergemålløsning(godkjenningsmeldingId)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "FERDIG"
        )
    }

    @Test
    fun `oppretter ikke ny oppgave når godkjenningsbehov kommer inn på nytt, og oppgaven er ferdigstilt`() {
        håndterVergeflyt(
            snapshot = SNAPSHOTV1_MED_WARNINGS // Legger på warning for at saken ikke skal automatiseres
        )
        val løsningId =
            sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        assertTilstand(løsningId, "NY", "FERDIG")
    }

    @Test
    fun `avbryter suspendert kommando når godkjenningsbehov kommer inn på nytt`() {
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_UTEN_WARNINGS
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val hendelseId1 = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        håndterVergeflyt(
            snapshot = null
        )
        assertTilstand(hendelseId1, "NY", "SUSPENDERT", "AVBRUTT")
    }

    @Test
    fun `ignorerer påminnet godkjenningsbehov dersom det eksisterer en aktiv oppgave`() {
        håndterVergeflyt()
        testRapid.reset()
        sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        assertTrue(testRapid.inspektør.behov().isEmpty())
    }

    @Test
    fun `ignorerer påminnet godkjenningsbehov dersom vedtaket er automatisk godkjent`() {
        val hendelseId = håndterVergeflyt()
        sendRisikovurderingløsning(hendelseId, VEDTAKSPERIODE_ID)
        assertOppgaver(0)
        assertAutomatisertLøsning()

        testRapid.reset()
        sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        assertTrue(testRapid.inspektør.behov().isEmpty())
    }

    @Test
    fun `reserverer person ved godkjenning av oppgave`() {
        saksbehandlerDao.opprettSaksbehandler(
            SAKSBEHANDLEROID,
            "Navn Navnesen",
            SAKSBEHANDLEREPOST,
            SAKSBEHANDLERIDENT
        )

        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_MED_WARNINGS
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsning(godkjenningsmeldingId)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)

        val (saksbehandler, gyldigTil) = reservasjonDao.hentReservasjonFor(FØDSELSNUMMER)!!

        assertEquals(SAKSBEHANDLEROID, saksbehandler)
        assertTrue(gyldigTil.isAfter(LocalDateTime.now()))

        testRapid.reset()

        val VEDTAKSPERIODE_ID2 = UUID.randomUUID()
        val UTBETALING_ID2 = UUID.randomUUID()
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns snapshotv1MedWarnings(vedtaksperiodeId = VEDTAKSPERIODE_ID2)

        val godkjenningsmeldingId2 = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID2, UTBETALING_ID2)
        sendPersoninfoløsning(godkjenningsmeldingId2, ORGNR, VEDTAKSPERIODE_ID2)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId2,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID2
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId2,
            erEgenAnsatt = false
        )
        sendVergemålløsning(godkjenningsmeldingId2)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId2,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId2,
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId2,
            vedtaksperiodeId = VEDTAKSPERIODE_ID2,
        )
        sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)

        val tildeling = tildelingDao.tildelingForOppgave(OPPGAVEID)
        assertEquals(SAKSBEHANDLEREPOST, tildeling?.epost)
    }

    @Test
    fun `legger ved alle orgnummere på behov for Arbeidsgiverinformasjon`() {
        val orgnummereMedRelevanteArbeidsforhold = listOf("123456789")
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_UTEN_WARNINGS
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold
        )
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)

        val orgnummere =
            testRapid.inspektør.meldinger().last()["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map { it.asText() }
        assertEquals(listOf(ORGNR) + orgnummereMedRelevanteArbeidsforhold, orgnummere)
    }

    @Test
    fun `skiller arbeidsgiverinformasjon- og personinfo-behov etter om det er et orgnr eller ikke`() {
        val orgnr1 = "123456789"
        val fnr1 = "12345678911"
        val orgnummereMedRelevanteArbeidsforhold = listOf(orgnr1, fnr1)
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_UTEN_WARNINGS
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold
        )
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)

        val sisteMelding = testRapid.inspektør.meldinger().last()
        assertTrue("Arbeidsgiverinformasjon" in sisteMelding.path("@behov").map { it.asText() })
        assertTrue("HentPersoninfoV2" in sisteMelding.path("@behov").map { it.asText() })
        val orgnummere = sisteMelding["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map { it.asText() }
        val identer = sisteMelding["HentPersoninfoV2"]["ident"].map { it.asText() }
        assertEquals(listOf(ORGNR, orgnr1), orgnummere)
        assertEquals(listOf(fnr1), identer)
    }

    @Test
    fun `tar inn arbeidsgiverinformasjon- og personinfo-behov samtidig`() {
        val orgnr1 = "123456789"
        val fnr1 = "12345678911"
        val orgnummereMedRelevanteArbeidsforhold = listOf(orgnr1, fnr1)
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_UTEN_WARNINGS
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold
        )
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendKomposittbehov(godkjenningsmeldingId, listOf("HentPersoninfoV2", "Arbeidsgiverinformasjon"), VEDTAKSPERIODE_ID, ORGNR, detaljer = mapOf(
            "@løsning" to mapOf(
                    "HentPersoninfoV2" to listOf(meldingsfabrikk.lagHentPersoninfoV2(fnr1)),
                    "Arbeidsgiverinformasjon" to listOf(
                            meldingsfabrikk.arbeidsgiverinformasjon(ORGNR, "Arbeidsgiver 1", emptyList()),
                            meldingsfabrikk.arbeidsgiverinformasjon(orgnr1, "Arbeidsgiver 2", emptyList())
                    )
            )
        ))
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT")
        val sisteMelding = testRapid.inspektør.meldinger().last()
        assertTrue("Arbeidsforhold" in sisteMelding.path("@behov").map { it.asText() })
    }

    @Test
    fun `legger til riktig felt for adressebeskyttelse i Personinfo`() {
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_UTEN_WARNINGS
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID, adressebeskyttelse = ADRESSEBESKYTTELSE.name
        )

        assertAdressebeskyttelse(FØDSELSNUMMER, ADRESSEBESKYTTELSE.name)
    }

    @Test
    fun `avbryter saksbehandling og avviser godkjenning på person med verge`() {
        håndterVergeflyt(VergemålJson(vergemål = listOf(Vergemål(voksen))))
        val godkjenning = testRapid.inspektør.løsning("Godkjenning")
        assertFalse(godkjenning["godkjent"].booleanValue())
        assertEquals("Vergemål", godkjenning["begrunnelser"].first().asText())
        assertTrue(testRapid.inspektør.behov().contains("Vergemål"))
        assertNotNull(testRapid.inspektør.hendelser("behov").firstOrNull { it.hasNonNull("Vergemål") })
    }

    @Test
    fun `avbryter ikke saksbehandling for person uten verge`() {
        håndterVergeflyt(VergemålJson())
        val godkjenning = testRapid.inspektør.løsning("Godkjenning")
        assertTrue(godkjenning["godkjent"].booleanValue())
        assertTrue(testRapid.inspektør.behov().contains("Vergemål"))
        assertNotNull(testRapid.inspektør.hendelser("behov").firstOrNull { it.hasNonNull("Vergemål") })
    }

    @Test
    fun `sendes til saksbehandler for godkjenning ved fullmakt`() {
        håndterVergeflyt(
            VergemålJson(
                fullmakter = listOf(
                    VergemålJson.Fullmakt(
                        områder = listOf(VergemålJson.Område.Syk),
                        gyldigTilOgMed = LocalDate.now(),
                        gyldigFraOgMed = LocalDate.now()
                    )
                )
            )
        )
        assertTrue(testRapid.inspektør.behov().contains("Vergemål"))
        assertNotNull(testRapid.inspektør.hendelser("behov").firstOrNull { it.hasNonNull("Vergemål") })
        assertThrows<NoSuchElementException> { testRapid.inspektør.løsning("Godkjenning") }
        sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        val godkjenning = testRapid.inspektør.løsning("Godkjenning")
        assertTrue(godkjenning["godkjent"].booleanValue())
        assertFalse(godkjenning["automatiskBehandling"].booleanValue())
        assertWarning("Registert fullmakt på personen.", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `sendes til saksbehandler for godkjenning ved fremtidsfullmakt`() {
        håndterVergeflyt(
            VergemålJson(
                fremtidsfullmakter = listOf(Vergemål(voksen))
            )
        )
        assertTrue(testRapid.inspektør.behov().contains("Vergemål"))
        assertNotNull(testRapid.inspektør.hendelser("behov").firstOrNull { it.hasNonNull("Vergemål") })
        assertThrows<NoSuchElementException> { testRapid.inspektør.løsning("Godkjenning") }
        sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        val godkjenning = testRapid.inspektør.løsning("Godkjenning")
        assertTrue(godkjenning["godkjent"].booleanValue())
        assertFalse(godkjenning["automatiskBehandling"].booleanValue())
        assertWarning("Registert fullmakt på personen.", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `avbryter saksbehandling og avvise godkjenning pga vergemål og egen ansatt`() {
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_UTEN_WARNINGS
        every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(godkjenningsmeldingId, true)
        sendVergemålløsning(
            godkjenningsmeldingId, vergemål = VergemålJson(
                vergemål = listOf(
                    Vergemål(
                        voksen
                    )
                )
            )
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "FERDIG"
        )
        assertEquals(1, testRapid.inspektør.hendelser("behov")
            .filter { it.hasNonNull("@løsning") }
            .filter {
                it.path("@behov")
                    .map(JsonNode::asText).contains("Godkjenning")
            }.size
        )
    }

    private fun håndterVergeflyt(
        vergemål: VergemålJson = VergemålJson(),
        snapshot: String? = SNAPSHOTV1_UTEN_WARNINGS) : UUID {
        snapshot?.also {
            every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns it
            every { graphqlClient.hentSnapshot(FØDSELSNUMMER) } returns graphQLSnapshot(FØDSELSNUMMER, AKTØR)
        }
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vergemål = vergemål
        )

        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "FERDIG"
        )
        snapshot?.also {
            assertSnapshot(it, VEDTAKSPERIODE_ID)
        }
        assertVedtak(VEDTAKSPERIODE_ID)
        return godkjenningsmeldingId
    }

    private fun assertVedtaksperiodeGodkjentEvent(vedtaksperiodeGodkjentEvent: JsonNode) {
        assertEquals(VEDTAKSPERIODE_ID, UUID.fromString(vedtaksperiodeGodkjentEvent["vedtaksperiodeId"].asText()))
        assertEquals(
            Periodetype.FØRSTEGANGSBEHANDLING.name,
            vedtaksperiodeGodkjentEvent["periodetype"].asText()
        )
        assertEquals(SAKSBEHANDLERIDENT, vedtaksperiodeGodkjentEvent["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLEREPOST, vedtaksperiodeGodkjentEvent["saksbehandlerEpost"].asText())
    }
}
