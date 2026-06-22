package no.nav.helse.e2e

import no.nav.helse.e2e.AbstractE2ETest.Kommandokjedetilstand.AVBRUTT
import no.nav.helse.e2e.AbstractE2ETest.Kommandokjedetilstand.FERDIG
import no.nav.helse.e2e.AbstractE2ETest.Kommandokjedetilstand.NY
import no.nav.helse.e2e.AbstractE2ETest.Kommandokjedetilstand.SUSPENDERT
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class VedtaksperiodeReberegnetE2ETest : AbstractE2ETest() {
    @Test
    fun `avbryter saksbehandling før oppgave er opprettet til saksbehandling`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterVedtaksperiodeReberegnet()
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
            AVBRUTT,
        )
        assertSaksbehandleroppgaveBleIkkeOpprettet()
    }

    @Test
    fun `avbryter saksbehandling etter oppgave er opprettet til saksbehandling`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterVedtaksperiodeReberegnet()
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
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.Invalidert)
    }

    @Test
    fun `avbryter kommandokjede ved reberegning og oppretter oppgave hos saksbehandler andre runde`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterVedtaksperiodeReberegnet()
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
            AVBRUTT,
        )
        assertSaksbehandleroppgaveBleIkkeOpprettet()

        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = false,
            harOppdatertMetadata = true,
        )
        assertKommandokjedetilstander(
            sisteGodkjenningsbehovId,
            NY,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            SUSPENDERT,
            FERDIG,
        )
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.AvventerSaksbehandler)
    }

    @Test
    fun `avbryt ikke-eksisterende vedtaksperiode`() {
        assertDoesNotThrow {
            håndterVedtaksperiodeReberegnet()
        }
    }
}
