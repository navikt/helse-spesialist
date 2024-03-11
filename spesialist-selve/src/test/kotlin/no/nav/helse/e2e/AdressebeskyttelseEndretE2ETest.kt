package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.spesialist.api.person.Adressebeskyttelse.Fortrolig
import no.nav.helse.spesialist.api.person.Adressebeskyttelse.StrengtFortrolig
import org.junit.jupiter.api.Test

internal class AdressebeskyttelseEndretE2ETest : AbstractE2ETest() {
    @Test
    fun `oppdaterer adressebeskyttelse på en person vi kjenner til fra før`() {

        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterAdressebeskyttelseEndret()
        assertSisteEtterspurteBehov("HentPersoninfoV2")

        assertAdressebeskyttelse(adressebeskyttelse = null)
        håndterPersoninfoløsning(adressebeskyttelse = Fortrolig)
        assertAdressebeskyttelse(adressebeskyttelse = Fortrolig)
    }

    @Test
    fun `oppdaterer ikke adressebeskyttelse dersom vi ikke kjenner til fødselsnummer`() {
        håndterAdressebeskyttelseEndret()
        assertIngenEtterspurteBehov()
    }

    @Test
    fun `Etterspør personinfo uten å sjekke ferskhet når adressebeskyttelse har blitt endret`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()

        // Etterspør personinfo selv om det nettopp er gjort
        håndterAdressebeskyttelseEndret()
        assertSisteEtterspurteBehov("HentPersoninfoV2")
    }

    @Test
    fun `Etterspør personinfo men avviser ikke når det ikke er noen åpne oppgaver`() {
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()

        håndterAdressebeskyttelseEndret()
        assertSisteEtterspurteBehov("HentPersoninfoV2")
        håndterPersoninfoløsning(adressebeskyttelse = StrengtFortrolig)
        assertIkkeUtgåendeMelding("vedtaksperiode_avvist")
    }

    @Test
    fun `Behandler ny strengt fortrolig adressebeskyttelse og avviser`() {
        fremTilSaksbehandleroppgave()
        håndterAdressebeskyttelseEndret()
        assertSisteEtterspurteBehov("HentPersoninfoV2")

        håndterPersoninfoløsning(adressebeskyttelse = StrengtFortrolig)
        assertUtgåendeMelding("vedtaksperiode_avvist")
    }

    @Test
    fun `Behandler ny fortrolig adressebeskyttelse og avviser ikke`() {
        fremTilSaksbehandleroppgave()
        håndterAdressebeskyttelseEndret()
        assertSisteEtterspurteBehov("HentPersoninfoV2")

        håndterPersoninfoløsning(adressebeskyttelse = Fortrolig)
        assertIkkeUtgåendeMelding("vedtaksperiode_avvist")
    }
}
