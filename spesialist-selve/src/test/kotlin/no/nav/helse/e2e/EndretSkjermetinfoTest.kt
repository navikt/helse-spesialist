package no.nav.helse.e2e

import AbstractE2ETestV2
import no.nav.helse.Testdata.FØDSELSNUMMER
import org.junit.jupiter.api.Test

internal class EndretSkjermetinfoTest : AbstractE2ETestV2() {

    @Test
    fun `Ignorerer hendelsen for ukjente personer`() {
        håndterEndretSkjermetinfo(skjermet = true)
        assertSkjermet(FØDSELSNUMMER, null)
    }

    @Test
    fun `Ignorerer hendelsen for fødselsnummer som ikke lar seg caste til long`() {
        håndterEndretSkjermetinfo("123456789XX", true)
        assertSkjermet(FØDSELSNUMMER, null)
    }

    @Test
    fun `Etterspør skjermetinfo for kjente personer hvor skjermetinfo mangler i basen`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtaksperiodeNyUtbetaling()
        håndterGodkjenningsbehov()
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()
        håndterEndretSkjermetinfo(skjermet = false)
        assertSkjermet(FØDSELSNUMMER, false)
        håndterEndretSkjermetinfo(skjermet = true)
        assertSkjermet(FØDSELSNUMMER, true)
    }
}
