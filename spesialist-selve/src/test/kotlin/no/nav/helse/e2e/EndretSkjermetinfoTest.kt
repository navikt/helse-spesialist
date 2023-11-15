package no.nav.helse.e2e

import AbstractE2ETest
import java.util.UUID
import no.nav.helse.TestRapidHelpers.behov
import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.januar
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class EndretSkjermetinfoTest : AbstractE2ETest() {

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

    @Test
    fun `Hva skjer i EgenAnsattCommand når vi allerede HAR oppdatert info`() {
        nyttVedtak(fom = 1.januar, tom = 20.januar)

        håndterEndretSkjermetinfo(skjermet = true)

        resetTestRapid()
        val vedtaksperiodeId2 = UUID.randomUUID()

        fremForbiUtbetalingsfilter(
            fom = 21.januar,
            tom = 30.januar,
            skjæringstidspunkt = 1.januar,
            harOppdatertMetadata = true,
            vedtaksperiodeId = vedtaksperiodeId2,
            utbetalingId = UUID.randomUUID(),
        )
        håndterEgenansattløsning(erEgenAnsatt = true)
        håndterVergemålløsning()
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning(vedtaksperiodeId = vedtaksperiodeId2)

        val behov = inspektør.behov("EgenAnsatt")
        assertEquals(1, behov.size) // Denne kan vi kanskje prøve å få ned til 0?
    }
}
