package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import java.util.UUID
import no.nav.helse.Meldingssender.sendArbeidsforholdløsningOld
import no.nav.helse.Meldingssender.sendArbeidsgiverinformasjonløsningOld
import no.nav.helse.Meldingssender.sendDigitalKontaktinformasjonløsningOld
import no.nav.helse.Meldingssender.sendEgenAnsattløsningOld
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendInntektløsningOld
import no.nav.helse.Meldingssender.sendPersoninfoløsningComposite
import no.nav.helse.Meldingssender.sendRisikovurderingløsningOld
import no.nav.helse.Meldingssender.sendUtbetalingEndret
import no.nav.helse.Meldingssender.sendVergemålløsningOld
import no.nav.helse.Meldingssender.sendÅpneGosysOppgaverløsningOld
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.SNAPSHOT_MED_WARNINGS
import no.nav.helse.Testdata.SNAPSHOT_UTEN_WARNINGS
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            periodetype = Periodetype.FORLENGELSE
        )
        sendPersoninfoløsningComposite(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            navn = "En Arbeidsgiver",
            bransjer = listOf("En eller flere bransjer")
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
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
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
            "SUSPENDERT",
            "FERDIG"
        )
        assertAutomatisertLøsning()
        val vedtaksperiodeGodkjentEvent =
            testRapid.inspektør.hendelser("vedtaksperiode_godkjent").firstOrNull()
        assertNotNull(vedtaksperiodeGodkjentEvent)
        if (vedtaksperiodeGodkjentEvent != null) {
            val vedtaksperiodeGodkjentEventBoolean: Boolean =
                vedtaksperiodeGodkjentEvent["automatiskBehandling"].asBoolean()
            assertTrue(vedtaksperiodeGodkjentEventBoolean)
        }
    }

    @Test
    fun `fatter automatisk vedtak ved infotrygdforlengelse`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            periodetype = Periodetype.INFOTRYGDFORLENGELSE
        )
        sendPersoninfoløsningComposite(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            navn = "En Arbeidsgiver",
            bransjer = listOf("En eller flere bransjer")
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
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
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
            "SUSPENDERT",
            "FERDIG"
        )
        assertAutomatisertLøsning()
    }

    @Test
    fun `fatter ikke automatisk vedtak ved warnings`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_MED_WARNINGS
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            periodetype = Periodetype.FORLENGELSE
        )
        sendPersoninfoløsningComposite(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            navn = "En Arbeidsgiver",
            bransjer = listOf("En eller flere bransjer")
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
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
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
        val løsningId = sendSaksbehandlerløsningFraAPI(
            oppgaveId = OPPGAVEID,
            saksbehandlerIdent = SAKSBEHANDLERIDENT,
            saksbehandlerEpost = SAKSBEHANDLEREPOST,
            saksbehandlerOid = SAKSBEHANDLEROID,
            godkjent = true
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
            Risikofunn(kategori = listOf("8-4"), beskrivelse = "8-4 ikke ok", kreverSupersaksbehandler = false),
            Risikofunn(kategori = emptyList(), beskrivelse = "faresignaler ikke ok", kreverSupersaksbehandler = false)
        )

        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            periodetype = Periodetype.FORLENGELSE
        )
        sendPersoninfoløsningComposite(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            navn = "En Arbeidsgiver",
            bransjer = listOf("En eller flere bransjer")
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
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsningOld(
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            godkjenningsmeldingId = godkjenningsmeldingId,
            kanGodkjennesAutomatisk = false,
            funn = funn
        )
        sendInntektløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
        )
        val løsningId = sendSaksbehandlerløsningFraAPI(
            oppgaveId = OPPGAVEID,
            saksbehandlerIdent = SAKSBEHANDLERIDENT,
            saksbehandlerEpost = SAKSBEHANDLEREPOST,
            saksbehandlerOid = SAKSBEHANDLEROID,
            godkjent = true
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
            "SUSPENDERT",
            "FERDIG"
        )
        assertTilstand(løsningId, "NY", "FERDIG")
        assertOppgavestatuser(
            0,
            Oppgavestatus.AvventerSaksbehandler,
            Oppgavestatus.AvventerSystem,
            Oppgavestatus.Ferdigstilt
        )
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
    fun `fatter ikke automatisk vedtak når bruker har åpne oppgaver i gosys`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            periodetype = Periodetype.FORLENGELSE
        )
        sendPersoninfoløsningComposite(
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            hendelseId = godkjenningsmeldingId
        )
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            navn = "En Arbeidsgiver",
            bransjer = listOf("En eller flere bransjer")
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
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            antall = 1,
            oppslagFeilet = false
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendInntektløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
        )
        val løsningId = sendSaksbehandlerløsningFraAPI(
            oppgaveId = OPPGAVEID,
            saksbehandlerIdent = SAKSBEHANDLERIDENT,
            saksbehandlerEpost = SAKSBEHANDLEREPOST,
            saksbehandlerOid = SAKSBEHANDLEROID,
            godkjent = true
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
            "SUSPENDERT",
            "FERDIG"
        )
        assertTilstand(løsningId, "NY", "FERDIG")
        assertOppgavestatuser(
            0,
            Oppgavestatus.AvventerSaksbehandler,
            Oppgavestatus.AvventerSystem,
            Oppgavestatus.Ferdigstilt
        )
        assertGodkjenningsbehovløsning(godkjent = true, saksbehandlerIdent = SAKSBEHANDLERIDENT)
    }
}
