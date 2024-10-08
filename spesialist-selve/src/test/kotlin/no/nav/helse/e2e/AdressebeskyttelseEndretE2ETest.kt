package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.spesialist.api.person.Adressebeskyttelse.Fortrolig
import no.nav.helse.spesialist.api.person.Adressebeskyttelse.StrengtFortrolig
import no.nav.helse.spesialist.api.person.Adressebeskyttelse.StrengtFortroligUtland
import org.junit.jupiter.api.Test

internal class AdressebeskyttelseEndretE2ETest : AbstractE2ETest() {
    @Test
    fun `oppdaterer adressebeskyttelse på en person vi kjenner til fra før`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
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
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()

        // Etterspør personinfo selv om det nettopp er gjort
        håndterAdressebeskyttelseEndret()
        assertSisteEtterspurteBehov("HentPersoninfoV2")
    }

    @Test
    fun `Etterspør personinfo men avviser ikke når det ikke er noen åpne oppgaver`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()

        håndterAdressebeskyttelseEndret()
        assertSisteEtterspurteBehov("HentPersoninfoV2")
        håndterPersoninfoløsning(adressebeskyttelse = StrengtFortrolig)
        assertIkkeUtgåendeMelding("vedtaksperiode_avvist")
    }

    @Test
    fun `Behandler ny strengt fortrolig adressebeskyttelse og avviser`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterAdressebeskyttelseEndret()
        assertSisteEtterspurteBehov("HentPersoninfoV2")

        håndterPersoninfoløsning(adressebeskyttelse = StrengtFortrolig)
        assertUtgåendeMelding("vedtaksperiode_avvist")
    }

    @Test
    fun `Behandler ny adressebeskyttelse strengt fortrolig utland og avviser`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterAdressebeskyttelseEndret()
        assertSisteEtterspurteBehov("HentPersoninfoV2")

        håndterPersoninfoløsning(adressebeskyttelse = StrengtFortroligUtland)
        assertUtgåendeMelding("vedtaksperiode_avvist")
    }

    @Test
    fun `Behandler ny fortrolig adressebeskyttelse og avviser ikke`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterAdressebeskyttelseEndret()
        assertSisteEtterspurteBehov("HentPersoninfoV2")

        håndterPersoninfoløsning(adressebeskyttelse = Fortrolig)
        assertIkkeUtgåendeMelding("vedtaksperiode_avvist")
    }
}
