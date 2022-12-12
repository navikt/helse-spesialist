package no.nav.helse.e2e

import AbstractE2ETestV2
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import org.junit.jupiter.api.Test

internal class AutomatiseringE2ETest : AbstractE2ETestV2() {
    @Test
    fun `fatter automatisk vedtak`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtaksperiodeNyUtbetaling()
        håndterGodkjenningsbehov()
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()
        håndterArbeidsgiverinformasjonløsning()
        håndterArbeidsforholdløsning()
        håndterEgenansattløsning()
        håndterVergemålløsning()
        håndterDigitalKontaktinformasjonløsning()
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()

        assertAutomatiskGodkjent()
    }

    @Test
    fun `fatter ikke automatisk vedtak ved warnings`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtaksperiodeNyUtbetaling()
        håndterGodkjenningsbehov()
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()
        håndterArbeidsgiverinformasjonløsning()
        håndterArbeidsforholdløsning(
            regelverksvarsler = listOf("Brukeren har flere inntekter de siste tre måneder")
        )
        håndterEgenansattløsning()
        håndterVergemålløsning()
        håndterDigitalKontaktinformasjonløsning()
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()

        assertIkkeAutomatiskGodkjent()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `fatter ikke automatisk vedtak ved 8-4 ikke oppfylt`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtaksperiodeNyUtbetaling()
        håndterGodkjenningsbehov()
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()
        håndterArbeidsgiverinformasjonløsning()
        håndterArbeidsforholdløsning()
        håndterEgenansattløsning()
        håndterVergemålløsning()
        håndterDigitalKontaktinformasjonløsning()
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning(
            risikofunn = listOf(
                Risikofunn(kategori = listOf("8-4"), beskrivelse = "8-4 ikke ok", kreverSupersaksbehandler = false),
                Risikofunn(kategori = emptyList(), beskrivelse = "faresignaler ikke ok", kreverSupersaksbehandler = false)
            ),
            kanGodkjennesAutomatisk = false
        )
        assertIkkeAutomatiskGodkjent()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `fatter ikke automatisk vedtak ved åpne oppgaver i gosys`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtaksperiodeNyUtbetaling()
        håndterGodkjenningsbehov()
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()
        håndterArbeidsgiverinformasjonløsning()
        håndterArbeidsforholdløsning()
        håndterEgenansattløsning()
        håndterVergemålløsning()
        håndterDigitalKontaktinformasjonløsning()
        håndterÅpneOppgaverløsning(antall = 1)
        håndterRisikovurderingløsning()

        assertIkkeAutomatiskGodkjent()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }
}
