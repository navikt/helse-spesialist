package no.nav.helse.e2e

import org.junit.jupiter.api.Test

class EndretEgenAnsattStatusTest : AbstractE2ETest() {
    @Test
    fun `Ignorerer hendelsen for ukjente personer`() {
        håndterEndretSkjermetinfo(skjermet = true)
        assertSkjermet(FØDSELSNUMMER, null)
    }

    @Test
    fun `Ignorerer hendelsen for fødselsnummer som ikke lar seg caste til long`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterEndretSkjermetinfo("123456789XX", true)
        assertSkjermet(FØDSELSNUMMER, null)
    }

    @Test
    fun `Oppdaterer egenansatt-status`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()

        håndterEndretSkjermetinfo(skjermet = false)
        assertSkjermet(FØDSELSNUMMER, false)

        håndterEndretSkjermetinfo(skjermet = true)
        assertSkjermet(FØDSELSNUMMER, true)
    }
}
