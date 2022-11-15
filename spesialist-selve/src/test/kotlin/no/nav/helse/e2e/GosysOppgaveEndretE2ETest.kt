package no.nav.helse.e2e

import AbstractE2ETest
import org.junit.jupiter.api.Test

internal class GosysOppgaveEndretE2ETest : AbstractE2ETest() {

    @Test
    fun `foo`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterGodkjenningsbehov()
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()
        håndterArbeidsgiverinformasjonløsning()
        håndterArbeidsforholdløsning()
        håndterEgenansattløsning()
        håndterVergemålløsning()
    }
}