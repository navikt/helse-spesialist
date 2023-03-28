package no.nav.helse.e2e

import AbstractE2ETest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.fasterxml.jackson.databind.JsonNode
import io.mockk.every
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Meldingssender.sendArbeidsforholdløsningOld
import no.nav.helse.Meldingssender.sendArbeidsgiverinformasjonløsningOld
import no.nav.helse.Meldingssender.sendEgenAnsattløsningOld
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendInntektløsningOld
import no.nav.helse.Meldingssender.sendKomposittbehov
import no.nav.helse.Meldingssender.sendPersoninfoløsningComposite
import no.nav.helse.Meldingssender.sendRisikovurderingløsningOld
import no.nav.helse.Meldingssender.sendSøknadSendt
import no.nav.helse.Meldingssender.sendUtbetalingEndret
import no.nav.helse.Meldingssender.sendVedtaksperiodeEndret
import no.nav.helse.Meldingssender.sendVedtaksperiodeForkastet
import no.nav.helse.Meldingssender.sendVedtaksperiodeNyUtbetaling
import no.nav.helse.Meldingssender.sendVergemålløsningOld
import no.nav.helse.Meldingssender.sendÅpneGosysOppgaverløsningOld
import no.nav.helse.TestRapidHelpers.behov
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.TestRapidHelpers.meldinger
import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.SNAPSHOT_MED_WARNINGS
import no.nav.helse.Testdata.SNAPSHOT_UTEN_WARNINGS
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Vergemål
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.VergemålType.voksen
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.snapshotMedWarnings
import no.nav.helse.spesialist.api.graphql.HentSnapshot
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSystem
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.Ferdigstilt
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GodkjenningE2ETest : AbstractE2ETest() {
    private companion object {
        private const val ENHET_UTLAND = "0393"
        private const val SAKSBEHANDLERIDENT = "Z999999"
        private const val AUTOMATISK_BEHANDLET = "Automatisk behandlet"
        private const val SAKSBEHANDLEREPOST = "saksbehandler@nav.no"
        private val SAKSBEHANDLEROID = UUID.randomUUID()
        private val ADRESSEBESKYTTELSE = Adressebeskyttelse.StrengtFortrolig
    }

    private val OPPGAVEID get() = testRapid.inspektør.oppgaveId()

    @Test
    fun `ignorerer endringer på ukjente vedtaksperioder`() {
        val hendelseId = sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID)
        assertIkkeHendelse(hendelseId)
        assertVedtaksperiodeEksistererIkke(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `ignorerer behov uten tilhørende command`() {
        val hendelseId = UUID.randomUUID()
        val contextId = UUID.randomUUID()
        sendPersoninfoløsningComposite(hendelseId, ORGNR, VEDTAKSPERIODE_ID, contextId)
        assertIkkeHendelse(hendelseId)
    }

    @Test
    fun `oppretter vedtak ved godkjenningsbehov`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, utbetalingId = UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        val godkjenningsmeldingId = sendGodkjenningsbehov(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(godkjenningsmeldingId, false)
        sendVergemålløsningOld(godkjenningsmeldingId)
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        assertSnapshot(SNAPSHOT_UTEN_WARNINGS, VEDTAKSPERIODE_ID)
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
            "FERDIG"
        )
        assertVedtaksperiodeEksisterer(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler godkjenner`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_MED_WARNINGS
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")

        val godkjenningsmeldingId = sendGodkjenningsbehov(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsningOld(godkjenningsmeldingId)
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendInntektløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
        )
        val løsningId =
            sendSaksbehandlerløsningFraAPI(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        sendUtbetalingEndret(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            utbetalingId = UTBETALING_ID,
            type = "UTBETALING",
            status = UTBETALT,
            arbeidsgiverFagsystemId = "EN_FAGSYSTEMID"
        )
        assertSnapshot(SNAPSHOT_MED_WARNINGS, VEDTAKSPERIODE_ID)
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
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_MED_WARNINGS //Legger på warning for at saken ikke skal automatiseres
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")

        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        val personinfomeldingId = sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        val arbeidsgiverløsningId = sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val arbeidsforholdmeldingId = sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val egenansattMeldingId = sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        val vergemålløsningId = sendVergemålløsningOld(godkjenningsmeldingId)
        val åpnegosysoppgaverløsningId = sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        val inntektløsningId = sendInntektløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
        )
        val risikoløsningId = sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val løsningId =
            sendSaksbehandlerløsningFraAPI(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)

        testRapid.inspektør.also { inspektør ->
            assertEquals(godkjenningsmeldingId.toString(), inspektør.field(0, "@forårsaket_av").path("id").asText())
            assertEquals(personinfomeldingId.toString(), inspektør.field(1, "@forårsaket_av").path("id").asText())
            assertEquals(arbeidsgiverløsningId.toString(), inspektør.field(2, "@forårsaket_av").path("id").asText())
            assertEquals(arbeidsforholdmeldingId.toString(), inspektør.field(3, "@forårsaket_av").path("id").asText())
            assertEquals(egenansattMeldingId.toString(), inspektør.field(4, "@forårsaket_av").path("id").asText())
            assertEquals(vergemålløsningId.toString(), inspektør.field(5, "@forårsaket_av").path("id").asText())
            assertEquals(
                åpnegosysoppgaverløsningId.toString(),
                inspektør.field(6, "@forårsaket_av").path("id").asText()
            )
            assertEquals(inntektløsningId.toString(), inspektør.field(7, "@forårsaket_av").path("id").asText())
            assertEquals(risikoløsningId.toString(), inspektør.field(8, "@forårsaket_av").path("id").asText())
            assertEquals(godkjenningsmeldingId.toString(), inspektør.field(10, "@forårsaket_av").path("id").asText())
            assertEquals(løsningId.toString(), inspektør.field(12, "@forårsaket_av").path("id").asText())
            assertNotEquals(godkjenningsmeldingId.toString(), inspektør.field(12, "@id").asText())

            0.until(inspektør.size).forEach {
                println(inspektør.message(it))
            }
        }
    }

    @Test
    fun `slår sammen warnings fra spleis og spesialist i utgående event`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_MED_WARNINGS
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsningOld(godkjenningsmeldingId)
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            antall = 1,
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendSaksbehandlerløsningFraAPI(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)

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
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_MED_WARNINGS //Legger på warning for at saken ikke skal automatiseres
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsningOld(godkjenningsmeldingId)
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendInntektløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
        )
        val begrunnelser = listOf("Fortjener ikke penger", "Skulker sannsynligvis")
        val kommentar = "Jeg har tatt meg litt frihet i vurderingen"
        val løsningId =
            sendSaksbehandlerløsningFraAPI(
                OPPGAVEID,
                SAKSBEHANDLERIDENT,
                SAKSBEHANDLEREPOST,
                SAKSBEHANDLEROID,
                false,
                begrunnelser,
                kommentar
            )
        sendUtbetalingEndret(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            utbetalingId = UTBETALING_ID,
            type = "UTBETALING",
            status = UTBETALT,
            arbeidsgiverFagsystemId = "EN_FAGSYSTEMID"
        )
        assertSnapshot(SNAPSHOT_MED_WARNINGS, VEDTAKSPERIODE_ID)
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
    fun `vedtaksperiode endret på aktiv saksbehandleroppgave`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returnsMany listOf(
            SNAPSHOT_UTEN_WARNINGS,
            SNAPSHOT_UTEN_WARNINGS
        )
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsningOld(godkjenningsmeldingId)
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            antall = 1,
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendInntektløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
        )
        assertOppgavestatuser(0, AvventerSaksbehandler)
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
        val endringsmeldingId = sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID)
        assertTilstand(endringsmeldingId, "NY", "FERDIG")
        assertSnapshot(SNAPSHOT_UTEN_WARNINGS, VEDTAKSPERIODE_ID)
        verify(exactly = 2) { snapshotClient.hentSnapshot(FØDSELSNUMMER) }
    }

    @Test
    fun `vedtaksperiode endret på oppgave som avventer system`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returnsMany listOf(
            SNAPSHOT_UTEN_WARNINGS,
            SNAPSHOT_UTEN_WARNINGS
        )

        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")

        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsningOld(godkjenningsmeldingId)
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            antall = 1,
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendInntektløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
        )
        sendSaksbehandlerløsningFraAPI(
            OPPGAVEID,
            SAKSBEHANDLERIDENT,
            SAKSBEHANDLEREPOST,
            SAKSBEHANDLEROID,
            true,
            listOf("begrunnelser"),
            "kommentar"
        )
        assertOppgavestatuser(0, AvventerSaksbehandler, AvventerSystem)
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
        val endringsmeldingId = sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID)
        assertTilstand(endringsmeldingId, "NY", "FERDIG")
        assertSnapshot(SNAPSHOT_UTEN_WARNINGS, VEDTAKSPERIODE_ID)
        verify(exactly = 2) { snapshotClient.hentSnapshot(FØDSELSNUMMER) }
    }

    @Test
    fun `vedtaksperiode endret på ferdigstilt oppgave`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returnsMany listOf(
            SNAPSHOT_UTEN_WARNINGS,
            SNAPSHOT_UTEN_WARNINGS
        )
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, utbetalingId = UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")

        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsningOld(godkjenningsmeldingId)
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            antall = 1,
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendInntektløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
        )
        sendSaksbehandlerløsningFraAPI(
            OPPGAVEID,
            SAKSBEHANDLERIDENT,
            SAKSBEHANDLEREPOST,
            SAKSBEHANDLEROID,
            true,
            listOf("begrunnelser"),
            "kommentar"
        )
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID)
        sendUtbetalingEndret(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            utbetalingId = UTBETALING_ID,
            type = "UTBETALING",
            status = UTBETALT,
            arbeidsgiverFagsystemId = "EN_FAGSYSTEMID"
        )
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID)
        assertOppgavestatuser(0, AvventerSaksbehandler, AvventerSystem, Ferdigstilt)
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
        verify(exactly = 3) { snapshotClient.hentSnapshot(FØDSELSNUMMER) }
    }

    @Test
    fun `vedtaksperiode forkastet`() {
        val hendelseId = sendVedtaksperiodeForkastet(ORGNR, VEDTAKSPERIODE_ID)
        assertIkkeHendelse(hendelseId)
        assertVedtaksperiodeEksistererIkke(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `gjør ingen ting om man får tilbake løsning på en avbrutt command context`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID)
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, utbetalingId = UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        val hendelseId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(hendelseId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = hendelseId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = hendelseId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendVedtaksperiodeForkastet(ORGNR, VEDTAKSPERIODE_ID)

        assertTilstand(hendelseId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "AVBRUTT")
        sendPersoninfoløsningComposite(hendelseId, ORGNR, VEDTAKSPERIODE_ID)
        assertTilstand(hendelseId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "AVBRUTT")
    }

    @Test
    fun `oppretter ikke oppgave om bruker er egen ansatt`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, utbetalingId = UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(godkjenningsmeldingId, true)
        sendVergemålløsningOld(godkjenningsmeldingId)
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        assertSnapshot(SNAPSHOT_UTEN_WARNINGS, VEDTAKSPERIODE_ID)
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
            "FERDIG"
        )
        assertVedtaksperiodeEksisterer(VEDTAKSPERIODE_ID)
        assertIngenOppgave()
        assertGodkjenningsbehovløsning(false, AUTOMATISK_BEHANDLET)
    }

    @Test
    fun `oppretter ikke oppgave om bruker tilhører utlandsenhet`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, utbetalingId = UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID, enhet = ENHET_UTLAND)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(godkjenningsmeldingId, false)
        sendVergemålløsningOld(godkjenningsmeldingId)
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        assertSnapshot(SNAPSHOT_UTEN_WARNINGS, VEDTAKSPERIODE_ID)
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
            "FERDIG"
        )
        assertVedtaksperiodeEksisterer(VEDTAKSPERIODE_ID)
        assertIngenOppgave()
        assertGodkjenningsbehovløsning(false, AUTOMATISK_BEHANDLET)
    }

    @Test
    fun `vanlig arbeidsforhold`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(godkjenningsmeldingId, false)
        sendVergemålløsningOld(godkjenningsmeldingId)
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsningOld(
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
            "FERDIG"
        )
    }

    @Test
    fun `oppretter ikke ny oppgave når godkjenningsbehov kommer inn på nytt, og oppgaven er ferdigstilt`() {
        håndterVergeflyt(
            snapshot = SNAPSHOT_MED_WARNINGS // Legger på warning for at saken ikke skal automatiseres
        )
        val løsningId =
            sendSaksbehandlerløsningFraAPI(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        assertTilstand(løsningId, "NY", "FERDIG")
    }

    @Test
    fun `avbryter suspendert kommando når godkjenningsbehov kommer inn på nytt`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID)
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, utbetalingId = UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        val hendelseId1 = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        håndterVergeflyt(snapshot = null, automatisertEllerAvvist = true)
        assertTilstand(hendelseId1, "NY", "SUSPENDERT", "AVBRUTT")
    }

    @Test
    fun `ignorerer påminnet godkjenningsbehov dersom det eksisterer en aktiv oppgave`() {
        håndterVergeflyt(automatisertEllerAvvist = true)
        testRapid.reset()
        sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        assertTrue(testRapid.inspektør.behov().isEmpty())
    }

    @Test
    fun `ignorerer påminnet godkjenningsbehov dersom vedtaket er automatisk godkjent`() {
        val hendelseId = håndterVergeflyt(automatisertEllerAvvist = true)
        sendRisikovurderingløsningOld(hendelseId, VEDTAKSPERIODE_ID)
        assertOppgaver(0)
        assertAutomatisertLøsning()

        testRapid.reset()
        sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
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

        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_MED_WARNINGS
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsningOld(godkjenningsmeldingId)
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendSaksbehandlerløsningFraAPI(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)

        val saksbehandler = reservasjonDao.hentReservertTil(FØDSELSNUMMER)!!

        assertEquals(SAKSBEHANDLEROID, saksbehandler)

        testRapid.reset()

        val VEDTAKSPERIODE_ID2 = UUID.randomUUID()
        val UTBETALING_ID2 = UUID.randomUUID()
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID2, forrigeTilstand = "START")
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID2, utbetalingId = UTBETALING_ID2, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID2, "UTBETALING")
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshotMedWarnings(
            vedtaksperiodeId = VEDTAKSPERIODE_ID2,
            orgnr = ORGNR,
            fnr = FØDSELSNUMMER,
            aktørId = AKTØR,
            utbetalingId = UTBETALING_ID2,
        )

        val godkjenningsmeldingId2 = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID2,
            utbetalingId = UTBETALING_ID2
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId2, ORGNR, VEDTAKSPERIODE_ID2)
        sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId2,
            erEgenAnsatt = false
        )
        sendVergemålløsningOld(godkjenningsmeldingId2)
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId2,
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId2,
            vedtaksperiodeId = VEDTAKSPERIODE_ID2,
        )

        val tildeling = tildelingDao.tildelingForOppgave(OPPGAVEID)
        assertEquals(SAKSBEHANDLEREPOST, tildeling?.epost)
    }

    @Test
    fun `legger ved alle orgnummere på behov for Arbeidsgiverinformasjon`() {
        val orgnummereMedRelevanteArbeidsforhold = listOf("123456789")
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID)
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, utbetalingId = UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)

        val orgnummere =
            testRapid.inspektør.meldinger().last()["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map { it.asText() }
        assertTrue((listOf(ORGNR) + orgnummereMedRelevanteArbeidsforhold).containsAll(orgnummere))
    }

    @Test
    fun `skiller arbeidsgiverinformasjon- og personinfo-behov etter om det er et orgnr eller ikke`() {
        val orgnr1 = "123456789"
        val fnr1 = "12345678911"
        val orgnummereMedRelevanteArbeidsforhold = listOf(orgnr1, fnr1)
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID)
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, utbetalingId = UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)

        val sisteMelding = testRapid.inspektør.meldinger().last()
        assertTrue("Arbeidsgiverinformasjon" in sisteMelding.path("@behov").map { it.asText() })
        assertTrue("HentPersoninfoV2" in sisteMelding.path("@behov").map { it.asText() })
        val orgnummere = sisteMelding["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map { it.asText() }
        val identer = sisteMelding["HentPersoninfoV2"]["ident"].map { it.asText() }
        assertTrue(listOf(ORGNR, orgnr1).containsAll(orgnummere))
        assertEquals(listOf(fnr1), identer)
    }

    @Test
    fun `tar inn arbeidsgiverinformasjon- og personinfo-behov samtidig`() {
        val orgnr1 = "123456789"
        val fnr1 = "12345678911"
        val orgnummereMedRelevanteArbeidsforhold = listOf(orgnr1, fnr1)
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID)
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, utbetalingId = UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendKomposittbehov(
            godkjenningsmeldingId,
            listOf("HentPersoninfoV2", "Arbeidsgiverinformasjon"),
            VEDTAKSPERIODE_ID,
            ORGNR,
            detaljer = mapOf(
                "@løsning" to mapOf(
                    "HentPersoninfoV2" to listOf(meldingsfabrikk.lagHentPersoninfoV2(fnr1)),
                    "Arbeidsgiverinformasjon" to listOf(
                        meldingsfabrikk.arbeidsgiverinformasjon(orgnr1, "Arbeidsgiver 2", emptyList())
                    )
                )
            )
        )
        sendArbeidsgiverinformasjonløsningOld(
            godkjenningsmeldingId,
            ORGNR,
            VEDTAKSPERIODE_ID
        )
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT")
        val sisteMelding = testRapid.inspektør.meldinger().last()
        assertTrue("Arbeidsforhold" in sisteMelding.path("@behov").map { it.asText() })
    }

    @Test
    fun `legger til riktig felt for adressebeskyttelse i Personinfo`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID)
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, utbetalingId = UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID, adressebeskyttelse = ADRESSEBESKYTTELSE.name
        )

        assertAdressebeskyttelse(FØDSELSNUMMER, ADRESSEBESKYTTELSE.name)
    }

    @Test
    fun `avbryter saksbehandling og avviser godkjenning på person med verge`() {
        håndterVergeflyt(vergemål = VergemålJson(vergemål = listOf(Vergemål(voksen))), automatisertEllerAvvist = true)
        assertGodkjenningsbehovløsning(godkjent = false, saksbehandlerIdent = AUTOMATISK_BEHANDLET) {
            assertEquals("Vergemål", it["begrunnelser"].first().asText())
        }
        assertTrue(testRapid.inspektør.behov().contains("Vergemål"))
        assertNotNull(testRapid.inspektør.hendelser("behov").firstOrNull { it.hasNonNull("Vergemål") })
    }

    @Test
    fun `avbryter ikke saksbehandling for person uten verge`() {
        håndterVergeflyt(vergemål = VergemålJson(), automatisertEllerAvvist = true)
        assertGodkjenningsbehovløsning(godkjent = true, saksbehandlerIdent = AUTOMATISK_BEHANDLET)
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
        assertGodkjenningsbehovIkkeLøst()
        sendSaksbehandlerløsningFraAPI(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        assertGodkjenningsbehovløsning(godkjent = true, saksbehandlerIdent = SAKSBEHANDLERIDENT)
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
        assertGodkjenningsbehovIkkeLøst()
        sendSaksbehandlerløsningFraAPI(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        assertGodkjenningsbehovløsning(godkjent = true, saksbehandlerIdent = SAKSBEHANDLERIDENT)
        assertWarning("Registert fullmakt på personen.", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `avbryter saksbehandling og avvise godkjenning pga vergemål og egen ansatt`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, utbetalingId = UTBETALING_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(godkjenningsmeldingId, true)
        sendVergemålløsningOld(
            godkjenningsmeldingId, vergemål = VergemålJson(
                vergemål = listOf(
                    Vergemål(
                        voksen
                    )
                )
            )
        )
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsningOld(
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
        snapshot: GraphQLClientResponse<HentSnapshot.Result>? = SNAPSHOT_UTEN_WARNINGS,
        automatisertEllerAvvist: Boolean = false
    ): UUID {
        snapshot?.also {
            every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns it
        }
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID, organisasjonsnummer = ORGNR)
        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID, "UTBETALING")

        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vergemål = vergemål
        )
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )

        if (!automatisertEllerAvvist) sendInntektløsningOld(godkjenningsmeldingId)

        val tilstander = mutableListOf("NY")
        tilstander.addAll(listOf("SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT"))
        if (!automatisertEllerAvvist) tilstander.add("SUSPENDERT")
        tilstander.add("FERDIG")

        assertTilstand(
            godkjenningsmeldingId,
            *tilstander.toTypedArray()
        )
        snapshot?.also {
            assertSnapshot(it, VEDTAKSPERIODE_ID)
        }
        assertVedtaksperiodeEksisterer(VEDTAKSPERIODE_ID)
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
