package no.nav.helse.e2e

import AbstractE2ETest
import AbstractE2ETest.Kommandokjedetilstand.AVBRUTT
import AbstractE2ETest.Kommandokjedetilstand.FERDIG
import AbstractE2ETest.Kommandokjedetilstand.NY
import AbstractE2ETest.Kommandokjedetilstand.SUSPENDERT
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class VedtaksperiodeReberegnetE2ETest : AbstractE2ETest() {
    @Test
    fun `avbryter saksbehandling før oppgave er opprettet til saksbehandling`() {
        håndterSøknad()
        spleisOppretterNyBehandling()
        fremTilÅpneOppgaver()
        håndterVedtaksperiodeReberegnet()
        assertKommandokjedetilstander(
            sisteGodkjenningsbehovId,
            NY, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, AVBRUTT
        )
        assertSaksbehandleroppgaveBleIkkeOpprettet()
    }

    @Test
    fun `avbryter saksbehandling etter oppgave er opprettet til saksbehandling`() {
        fremTilSaksbehandleroppgave()
        håndterVedtaksperiodeReberegnet()
        assertKommandokjedetilstander(
            sisteGodkjenningsbehovId,
            NY, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, FERDIG
        )
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.Invalidert)
    }

    @Test
    fun `avbryter kommandokjede ved reberegning og oppretter oppgave hos saksbehandler andre runde`() {
        håndterSøknad()
        spleisOppretterNyBehandling()
        fremTilÅpneOppgaver()
        håndterVedtaksperiodeReberegnet()
        assertKommandokjedetilstander(
            sisteGodkjenningsbehovId,
            NY, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, AVBRUTT
        )
        assertSaksbehandleroppgaveBleIkkeOpprettet()

        fremTilSaksbehandleroppgave(harOppdatertMetadata = true, harRisikovurdering = false)
        assertKommandokjedetilstander(
            sisteGodkjenningsbehovId,
            NY, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, FERDIG
        )
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.AvventerSaksbehandler)
    }

    @Test
    fun `avbryt ikke-eksisterende vedtaksperiode`() {
        assertDoesNotThrow {
            håndterVedtaksperiodeReberegnet()
        }
    }

    @Test
    fun `avbryter ikke om forrige tilstand er noe annet enn AVVENTER_GODKJENNING eller AVVENTER_GODKJENNING_REVURDERING`() {
        håndterSøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()
        håndterVedtaksperiodeEndret(forrigeTilstand = "TIL_UTBETALING", gjeldendeTilstand = "UTBETALING_FEILET")
        assertKommandokjedetilstander(sisteGodkjenningsbehovId, NY, SUSPENDERT)
    }

    @Test
    fun `avbryter ikke om forrige tilstand er AVVENTER_GODKJENNING_REVURDERING og gjeldende tilstand er TIL_INFOTRYGD`() {
        håndterSøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()
        håndterVedtaksperiodeEndret(forrigeTilstand = "AVVENTER_GODKJENNING_REVURDERING", gjeldendeTilstand = "TIL_INFOTRYGD")
        assertKommandokjedetilstander(sisteGodkjenningsbehovId, NY, SUSPENDERT)
    }

    @Test
    fun `avbryter ikke om forrige tilstand er AVVENTER_GODKJENNING_REVURDERING og gjeldende tilstand er AVSLUTTET`() {
        håndterSøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()
        håndterVedtaksperiodeEndret(forrigeTilstand = "AVVENTER_GODKJENNING_REVURDERING", gjeldendeTilstand = "AVSLUTTET")
        assertKommandokjedetilstander(sisteGodkjenningsbehovId, NY, SUSPENDERT)
    }

    @Test
    fun `avbryter ikke om forrige tilstand er AVVENTER_GODKJENNING_REVURDERING og gjeldende tilstand er TIL_UTBETALING`() {
        håndterSøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()
        håndterVedtaksperiodeEndret(forrigeTilstand = "AVVENTER_GODKJENNING_REVURDERING", gjeldendeTilstand = "TIL_UTBETALING")
        assertKommandokjedetilstander(sisteGodkjenningsbehovId, NY, SUSPENDERT)
    }

    @Test
    fun `avbryter ikke om gjeldende tilstand er TIL_INFOTRYGD`() {
        håndterSøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()
        håndterVedtaksperiodeEndret(gjeldendeTilstand = "TIL_INFOTRYGD")
        assertKommandokjedetilstander(sisteGodkjenningsbehovId, NY, SUSPENDERT)
    }

    @Test
    fun `avbryter ikke om gjeldende tilstand er AVSLUTTET`() {
        håndterSøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()
        håndterVedtaksperiodeEndret(gjeldendeTilstand = "AVSLUTTET")
        assertKommandokjedetilstander(sisteGodkjenningsbehovId, NY, SUSPENDERT)
    }

    @Test
    fun `avbryter ikke om gjeldende tilstand er TIL_UTBETALING`() {
        håndterSøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()
        håndterVedtaksperiodeEndret(gjeldendeTilstand = "TIL_UTBETALING")
        assertKommandokjedetilstander(sisteGodkjenningsbehovId, NY, SUSPENDERT)
    }
}
