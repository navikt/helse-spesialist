package no.nav.helse.e2e

import AbstractE2ETestV2
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
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
    fun `Oppdaterer egenansatt-status`() {
        fremTilSaksbehandleroppgave()

        håndterEndretSkjermetinfo(skjermet = false)
        assertSkjermet(FØDSELSNUMMER, false)

        håndterEndretSkjermetinfo(skjermet = true)
        assertSkjermet(FØDSELSNUMMER, true)
    }

    @Test
    fun `Avbryter oppgave når person blir egen ansatt`() {
        fremTilSaksbehandleroppgave()
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.AvventerSaksbehandler)

        håndterEndretSkjermetinfo(skjermet = true)

        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.Invalidert)
        assertVedtaksperiodeAvvist("FØRSTEGANGSBEHANDLING", listOf("Egen ansatt"))
    }
}
