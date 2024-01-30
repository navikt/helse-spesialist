package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import org.junit.jupiter.api.Test

internal class EndretEgenAnsattStatusTest : AbstractE2ETest() {

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
    fun `Legger til egenskap på oppgave når person får status egen ansatt`() {
        fremTilSaksbehandleroppgave()

        val oppgaveId = inspektør.oppgaveId().toInt()
        assertHarIkkeOppgaveegenskap(oppgaveId, Egenskap.EGEN_ANSATT)

        håndterEndretSkjermetinfo(skjermet = true)
        assertHarOppgaveegenskap(oppgaveId, Egenskap.EGEN_ANSATT)
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.AvventerSaksbehandler)
    }

    @Test
    fun `Fjerner egenskap egen ansatt hvis personen ikke lenger har status egen ansatt`() {
        fremTilSaksbehandleroppgave()
        håndterEndretSkjermetinfo(skjermet = true)

        val oppgaveId = inspektør.oppgaveId().toInt()
        assertHarOppgaveegenskap(oppgaveId, Egenskap.EGEN_ANSATT)

        håndterEndretSkjermetinfo(skjermet = false)

        assertHarIkkeOppgaveegenskap(oppgaveId, Egenskap.EGEN_ANSATT)
    }
}
