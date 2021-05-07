package no.nav.helse.e2e

import AbstractE2ETest
import com.fasterxml.jackson.databind.JsonNode
import io.mockk.every
import io.mockk.verify
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.oppgave.Oppgavestatus.*
import no.nav.helse.snapshotMedWarning
import no.nav.helse.snapshotUtenWarnings
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class GodkjenningE2ETest : AbstractE2ETest() {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val ORGNR = "222222222"
        private const val ENHET_UTLAND = "2101"
        private const val SAKSBEHANDLERIDENT = "Z999999"
        private const val AUTOMATISK_BEHANDLET = "Automatisk behandlet"
        private const val SAKSBEHANDLEREPOST = "saksbehandler@nav.no"

        private val SAKSBEHANDLEROID = UUID.randomUUID()
        private val SNAPSHOT_MED_WARNINGS = snapshotMedWarning(VEDTAKSPERIODE_ID)
        private val SNAPSHOT_UTEN_WARNINGS = snapshotUtenWarnings(VEDTAKSPERIODE_ID)
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
        assertBehov("HentPersoninfo", "HentEnhet", "HentInfotrygdutbetalinger")
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
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOT_UTEN_WARNINGS
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
        assertVedtak(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler godkjenner`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOT_MED_WARNINGS //Legger på warning for at saken ikke skal automatiseres
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
            "FERDIG"
        )
        assertTilstand(løsningId, "NY", "FERDIG")
        assertOppgave(0, AvventerSaksbehandler, AvventerSystem, Ferdigstilt)
        assertGodkjenningsbehovløsning(true, SAKSBEHANDLERIDENT)
        assertNotNull(testRapid.inspektør.hendelser("vedtaksperiode_godkjent").firstOrNull())
    }

    @Test
    fun `slår sammen warnings fra spleis og spesialist i utgående event`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOT_MED_WARNINGS
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
        assertEquals(UNG_PERSON_FNR_2018, vedtaksperiodeGodkjentEvent["fødselsnummer"].asText())
        assertTrue(vedtaksperiodeGodkjentEvent.hasNonNull("@opprettet"))
        assertEquals(2, vedtaksperiodeGodkjentEvent["warnings"].size())
        assertEquals(WarningKilde.Spleis.name, vedtaksperiodeGodkjentEvent["warnings"][0]["kilde"].asText())
        assertEquals(WarningKilde.Spesialist.name, vedtaksperiodeGodkjentEvent["warnings"][1]["kilde"].asText())
        assertFalse(vedtaksperiodeGodkjentEvent["automatiskBehandling"].asBoolean())
        assertVedtaksperiodeGodkjentEvent(vedtaksperiodeGodkjentEvent)
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler avslår`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOT_MED_WARNINGS //Legger på warning for at saken ikke skal automatiseres
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
            sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, false)
        sendUtbetalingEndret("UTBETALING", UTBETALT, ORGNR, "EN_FAGSYSTEMID", utbetalingId = UTBETALING_ID)
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
            "FERDIG"
        )
        assertTilstand(løsningId, "NY", "FERDIG")
        assertOppgave(0, AvventerSaksbehandler, AvventerSystem, Ferdigstilt)
        assertGodkjenningsbehovløsning(false, SAKSBEHANDLERIDENT)
    }

    @Test
    fun `endringer på kjente vedtaksperioder`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returnsMany listOf(
            SNAPSHOT_UTEN_WARNINGS,
            SNAPSHOT_UTEN_WARNINGS
        )
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
        val endringsmeldingId = sendVedtaksperiodeEndret(ORGNR, VEDTAKSPERIODE_ID)
        assertTilstand(godkjenningsmeldingId, "NY", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT", "SUSPENDERT")
        assertTilstand(endringsmeldingId, "NY", "FERDIG")
        assertSnapshot(SNAPSHOT_UTEN_WARNINGS, VEDTAKSPERIODE_ID)
        verify(exactly = 2) { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) }
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
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOT_UTEN_WARNINGS
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
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOT_UTEN_WARNINGS
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
        assertVedtak(VEDTAKSPERIODE_ID)
        assertIngenOppgave()
        assertGodkjenningsbehovløsning(false, AUTOMATISK_BEHANDLET)
    }

    @Test
    fun `oppretter ikke oppgave om bruker tilhører utlandsenhet`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOT_UTEN_WARNINGS
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
        assertVedtak(VEDTAKSPERIODE_ID)
        assertIngenOppgave()
        assertGodkjenningsbehovløsning(false, AUTOMATISK_BEHANDLET)
    }

    @Test
    fun `vanlig arbeidsforhold`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOT_UTEN_WARNINGS
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
            "FERDIG"
        )
    }

    @Test
    fun `oppretter ikke ny oppgave når godkjenningsbehov kommer inn på nytt, og oppgaven er ferdigstilt`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOT_MED_WARNINGS //Legger på warning for at saken ikke skal automatiseres
        val hendelseId1 = håndterGodkjenningsbehov()
        val løsningId =
            sendSaksbehandlerløsning(OPPGAVEID, SAKSBEHANDLERIDENT, SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, true)
        assertTilstand(
            hendelseId1,
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
        assertTilstand(løsningId, "NY", "FERDIG")
    }

    @Test
    fun `avbryter suspendert kommando når godkjenningsbehov kommer inn på nytt`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOT_UTEN_WARNINGS
        val hendelseId1 = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        val hendelseId2 = håndterGodkjenningsbehov()
        assertTilstand(hendelseId1, "NY", "SUSPENDERT", "AVBRUTT")
        assertTilstand(
            hendelseId2,
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
    fun `ignorerer påminnet godkjenningsbehov dersom det eksisterer en aktiv oppgave`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOT_UTEN_WARNINGS
        håndterGodkjenningsbehov()

        testRapid.reset()
        sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        assertTrue(testRapid.inspektør.behov().isEmpty())
    }

    @Test
    fun `ignorerer påminnet godkjenningsbehov dersom vedtaket er automatisk godkjent`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns snapshotUtenWarnings(
            VEDTAKSPERIODE_ID
        )
        val hendelseId = håndterGodkjenningsbehov()
        sendRisikovurderingløsning(hendelseId, VEDTAKSPERIODE_ID)
        assertOppgaver(0)
        assertAutomatisertLøsning()

        testRapid.reset()
        sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        assertTrue(testRapid.inspektør.behov().isEmpty())
    }

    @Test
    fun `reserverer person ved godkjenning av oppgave`() {
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLEROID, "Navn Navnesen", SAKSBEHANDLEREPOST)

        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOT_MED_WARNINGS
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

        val (saksbehandler, gyldigTil) = reservasjonDao.hentReservasjonFor(UNG_PERSON_FNR_2018)!!

        assertEquals(SAKSBEHANDLEROID, saksbehandler)
        assertTrue(gyldigTil.isAfter(LocalDateTime.now()))

        testRapid.reset()

        val VEDTAKSPERIODE_ID2 = UUID.randomUUID()
        val UTBETALING_ID2 = UUID.randomUUID()
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns snapshotMedWarning(VEDTAKSPERIODE_ID2)

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

    private fun håndterGodkjenningsbehov(): UUID {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(godkjenningsmeldingId, false)
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
