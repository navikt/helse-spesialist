package no.nav.helse.spesialist.e2etests.legacy.flyttestilkafka

import no.nav.helse.e2e.AbstractE2ETest
import no.nav.helse.modell.person.Adressebeskyttelse
import org.junit.jupiter.api.Test

class AdressebeskyttelseEndretE2ETest : AbstractE2ETest() {
    @Test
    fun `oppdaterer ikke adressebeskyttelse dersom vi ikke kjenner til fødselsnummer`() {
        håndterAdressebeskyttelseEndret()
        assertIngenEtterspurteBehov()
    }

    @Test
    fun `Etterspør personinfo men avviser ikke når det ikke er noen åpne oppgaver`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterSaksbehandlerløsning()
        håndterAvsluttetMedVedtak()

        håndterAdressebeskyttelseEndret()
        assertSisteEtterspurteBehov("HentPersoninfoV2")
        håndterPersoninfoløsning(adressebeskyttelse = Adressebeskyttelse.StrengtFortrolig)
        assertIkkeUtgåendeMelding("vedtaksperiode_avvist")
    }

    @Test
    fun `Behandler ny strengt fortrolig adressebeskyttelse og avviser`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterAdressebeskyttelseEndret()
        assertSisteEtterspurteBehov("HentPersoninfoV2")

        håndterPersoninfoløsning(adressebeskyttelse = Adressebeskyttelse.StrengtFortrolig)
        assertUtgåendeMelding("vedtaksperiode_avvist")
    }

    @Test
    fun `Behandler ny adressebeskyttelse strengt fortrolig utland og avviser`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterAdressebeskyttelseEndret()
        assertSisteEtterspurteBehov("HentPersoninfoV2")

        håndterPersoninfoløsning(adressebeskyttelse = Adressebeskyttelse.StrengtFortroligUtland)
        assertUtgåendeMelding("vedtaksperiode_avvist")
    }

    @Test
    fun `Behandler ny fortrolig adressebeskyttelse og avviser ikke`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterAdressebeskyttelseEndret()
        assertSisteEtterspurteBehov("HentPersoninfoV2")

        håndterPersoninfoløsning(adressebeskyttelse = Adressebeskyttelse.Fortrolig)
        assertIkkeUtgåendeMelding("vedtaksperiode_avvist")
    }
}
