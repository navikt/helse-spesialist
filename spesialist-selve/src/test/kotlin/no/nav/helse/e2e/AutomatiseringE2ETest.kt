package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.oppgave.Oppgavestatus
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class AutomatiseringE2ETest : AbstractE2ETest() {
    private companion object {
        private const val SAKSBEHANDLERIDENT = "Z999999"
        private const val SAKSBEHANDLEREPOST = "saksbehandler@nav.no"
        private val SAKSBEHANDLEROID = UUID.randomUUID()
    }

    private val OPPGAVEID get() = testRapid.inspektør.oppgaveId()

    @Test
    fun `fatter automatisk vedtak`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            periodetype = Periodetype.FORLENGELSE,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsning(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
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
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
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
        assertAutomatisertLøsning()
        val vedtaksperiodeGodkjentEvent =
            assertNotNull(testRapid.inspektør.hendelser("vedtaksperiode_godkjent").firstOrNull())
        assertTrue(vedtaksperiodeGodkjentEvent["automatiskBehandling"].asBoolean())
    }

    @Test
    fun `fatter automatisk vedtak ved infotrygdforlengelse`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            periodetype = Periodetype.INFOTRYGDFORLENGELSE,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsning(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
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
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
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
        assertAutomatisertLøsning()
    }

    @Test
    fun `fatter ikke automatisk vedtak ved warnings`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_MED_WARNINGS
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            periodetype = Periodetype.FORLENGELSE,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsning(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
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
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
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
        val løsningId = sendSaksbehandlerløsning(
            oppgaveId = OPPGAVEID,
            saksbehandlerIdent = SAKSBEHANDLERIDENT,
            saksbehandlerEpost = SAKSBEHANDLEREPOST,
            saksbehandlerOid = SAKSBEHANDLEROID,
            godkjent = true
        )
        sendUtbetalingEndret("UTBETALING", UTBETALT, ORGNR, "EN_FAGSYSTEMID", utbetalingId = UTBETALING_ID)
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
        assertOppgavestatuser(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.AvventerSystem, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovløsning(godkjent = true, saksbehandlerIdent = SAKSBEHANDLERIDENT)
    }

    @Test
    fun `fatter ikke automatisk vedtak ved 8-4 ikke oppfylt`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS

        val funn = listOf(
            Risikofunn(kategori = listOf("8-4"), beskrivele = "8-4 ikke ok", kreverSupersaksbehandler = false),
            Risikofunn(kategori = emptyList(), beskrivele = "faresignaler ikke ok", kreverSupersaksbehandler = false)
        )

        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID,
            periodetype = Periodetype.FORLENGELSE,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsning(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
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
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            godkjenningsmeldingId = godkjenningsmeldingId,
            kanGodkjennesAutomatisk = false,
            funn = funn
        )
        val løsningId = sendSaksbehandlerløsning(
            oppgaveId = OPPGAVEID,
            saksbehandlerIdent = SAKSBEHANDLERIDENT,
            saksbehandlerEpost = SAKSBEHANDLEREPOST,
            saksbehandlerOid = SAKSBEHANDLEROID,
            godkjent = true
        )
        sendUtbetalingEndret("UTBETALING", UTBETALT, ORGNR, "EN_FAGSYSTEMID", utbetalingId = UTBETALING_ID)
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
        assertOppgavestatuser(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.AvventerSystem, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovløsning(godkjent = true, saksbehandlerIdent = SAKSBEHANDLERIDENT)
        assertWarning(
            """
                Arbeidsuførhet, aktivitetsplikt og/eller medvirkning må vurderes.
                8-4 ikke ok
            """.trimIndent(),
            VEDTAKSPERIODE_ID
        )
        assertWarning(
            "Faresignaler oppdaget. Kontroller om faresignalene påvirker retten til sykepenger.",
            VEDTAKSPERIODE_ID
        )
    }

    @Test
    fun `fatter ikke automatisk vedtak når bruker er ikke-digital`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            periodetype = Periodetype.FORLENGELSE,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsning(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
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
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = false
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val løsningId = sendSaksbehandlerløsning(
            oppgaveId = OPPGAVEID,
            saksbehandlerIdent = SAKSBEHANDLERIDENT,
            saksbehandlerEpost = SAKSBEHANDLEREPOST,
            saksbehandlerOid = SAKSBEHANDLEROID,
            godkjent = true
        )
        sendUtbetalingEndret("UTBETALING", UTBETALT, ORGNR, "EN_FAGSYSTEMID", utbetalingId = UTBETALING_ID)
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
        assertOppgavestatuser(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.AvventerSystem, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovløsning(godkjent = true, saksbehandlerIdent = SAKSBEHANDLERIDENT)
    }

    @Test
    fun `fatter ikke automatisk vedtak når bruker har åpne oppgaver i gosys`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            periodetype = Periodetype.FORLENGELSE,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsning(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
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
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            antall = 1,
            oppslagFeilet = false
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val løsningId = sendSaksbehandlerløsning(
            oppgaveId = OPPGAVEID,
            saksbehandlerIdent = SAKSBEHANDLERIDENT,
            saksbehandlerEpost = SAKSBEHANDLEREPOST,
            saksbehandlerOid = SAKSBEHANDLEROID,
            godkjent = true
        )
        sendUtbetalingEndret("UTBETALING", UTBETALT, ORGNR, "EN_FAGSYSTEMID", utbetalingId = UTBETALING_ID)
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
        assertOppgavestatuser(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.AvventerSystem, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovløsning(godkjent = true, saksbehandlerIdent = SAKSBEHANDLERIDENT)
    }
}
