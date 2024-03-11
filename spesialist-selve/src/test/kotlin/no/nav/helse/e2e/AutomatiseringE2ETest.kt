package no.nav.helse.e2e

import AbstractE2ETest
import ToggleHelpers.disable
import ToggleHelpers.enable
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.januar
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.Toggle
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class AutomatiseringE2ETest : AbstractE2ETest() {
    @Test
    fun `fatter automatisk vedtak`() {
        automatiskGodkjent()
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `fatter ikke automatisk vedtak ved warnings`() {
        fremTilSaksbehandleroppgave(
            regelverksvarsler = listOf("RV_IM_1"),
            kanGodkjennesAutomatisk = true
        )

        assertGodkjenningsbehovIkkeBesvart()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `fatter ikke automatisk vedtak ved 8-4 ikke oppfylt`() {
        fremTilSaksbehandleroppgave(
            risikofunn = listOf(
                Risikofunn(kategori = listOf("8-4"), beskrivelse = "8-4 ikke ok", kreverSupersaksbehandler = false),
                Risikofunn(kategori = emptyList(), beskrivelse = "faresignaler ikke ok", kreverSupersaksbehandler = false)
            ),
            kanGodkjennesAutomatisk = false
        )
        assertGodkjenningsbehovIkkeBesvart()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `fatter ikke automatisk vedtak ved åpne oppgaver i gosys`() {
        fremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antall = 1)
        håndterRisikovurderingløsning()

        assertGodkjenningsbehovIkkeBesvart()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `fatter automatisk vedtak dersom åpen oppgave får inn godkjent tilbakedatering`() {
        fremTilÅpneOppgaver(
            regelverksvarsler = listOf("RV_SØ_3"),
        )
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()

        assertGodkjenningsbehovIkkeBesvart()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)

        håndterTilbakedateringBehandlet(skjæringstidspunkt = 1.januar)
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `fatter automatisk vedtak for spesialsaker som ikke har svartelistede varsler og ingen utbetaling`() {
        Toggle.AutomatiserSpesialsak.enable()
        håndterSøknad()
        håndterVedtaksperiodeOpprettet(vedtaksperiodeId = VEDTAKSPERIODE_ID)
        opprettSpesialsak(vedtaksperiodeId = VEDTAKSPERIODE_ID)
        fremTilÅpneOppgaver(
            fullmakter = listOf(Testmeldingfabrikk.VergemålJson.Fullmakt(listOf(Testmeldingfabrikk.VergemålJson.Område.Syk), 1.januar, 31.januar)),
            regelverksvarsler = listOf("RV_IM_1"),
            arbeidsgiverbeløp = 0,
            personbeløp = 0
        )
        håndterÅpneOppgaverløsning(antall = 1)
        håndterRisikovurderingløsning(
            kanGodkjennesAutomatisk = false,
            risikofunn = listOf(
                Risikofunn(kategori = emptyList(), beskrivelse = "faresignaler ikke ok", kreverSupersaksbehandler = false)
            ),
        )

        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
        assertSaksbehandleroppgaveBleIkkeOpprettet()
        assertGodkjentVarsel(VEDTAKSPERIODE_ID, "RV_IM_1")
        assertGodkjentVarsel(VEDTAKSPERIODE_ID, "SB_RV_1")
        assertGodkjentVarsel(VEDTAKSPERIODE_ID, "SB_EX_1")
        assertGodkjentVarsel(VEDTAKSPERIODE_ID, "SB_IK_1")
        Toggle.AutomatiserSpesialsak.disable()
    }

    @Test
    fun `fatter ikke automatisk vedtak for spesialsaker som ikke har svartelistede varsler men har utbetaling`() {
        Toggle.AutomatiserSpesialsak.enable()
        håndterSøknad()
        håndterVedtaksperiodeOpprettet(vedtaksperiodeId = VEDTAKSPERIODE_ID)
        opprettSpesialsak(vedtaksperiodeId = VEDTAKSPERIODE_ID)
        fremTilSaksbehandleroppgave(
            risikofunn = listOf(
                Risikofunn(kategori = emptyList(), beskrivelse = "faresignaler ikke ok", kreverSupersaksbehandler = false)
            ),
            kanGodkjennesAutomatisk = false,
            fullmakter = listOf(Testmeldingfabrikk.VergemålJson.Fullmakt(listOf(Testmeldingfabrikk.VergemålJson.Område.Syk), 1.januar, 31.januar)),
            arbeidsgiverbeløp = 1000,
            personbeløp = 0
        )

        assertGodkjenningsbehovIkkeBesvart()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
        Toggle.AutomatiserSpesialsak.disable()
    }

    @Test
    fun `fatter ikke automatisk vedtak for spesialsaker som har svartelistede varsler men ingen utbetaling`() {
        Toggle.AutomatiserSpesialsak.enable()
        håndterSøknad()
        håndterVedtaksperiodeOpprettet(vedtaksperiodeId = VEDTAKSPERIODE_ID)
        opprettSpesialsak(vedtaksperiodeId = VEDTAKSPERIODE_ID)
        fremTilSaksbehandleroppgave(
            regelverksvarsler = listOf("RV_SI_3"),
            arbeidsgiverbeløp = 0,
            personbeløp = 0
        )

        assertGodkjenningsbehovIkkeBesvart()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
        Toggle.AutomatiserSpesialsak.disable()
    }

    private fun opprettSpesialsak(vedtaksperiodeId: UUID) {
        @Language("PostgreSQL")
        val query = """INSERT INTO spesialsak(vedtaksperiode_id) VALUES(?)"""
        sessionOf(Companion.dataSource).use {
            it.run(queryOf(query, vedtaksperiodeId).asExecute)
        }
    }
}
