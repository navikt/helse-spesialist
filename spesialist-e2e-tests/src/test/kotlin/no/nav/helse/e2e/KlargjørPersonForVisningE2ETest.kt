package no.nav.helse.e2e

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.Sekvensnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KlargjørPersonForVisningE2ETest : AbstractE2ETest() {

    @Test
    fun `Henter personinfo når person skal klargjøres for visning`() {
        val fødselsnummer = lagFødselsnummer()
        vedtaksløsningenMottarNySøknad(fødselsnummer = fødselsnummer)
        spleisOppretterNyBehandling(fødselsnummer = fødselsnummer)
        håndterSkalKlargjøresForVisning(fødselsnummer = fødselsnummer)
        assertSisteEtterspurteBehov("HentPersoninfoV2")
    }

    @Test
    fun `Henter enhet når person skal klargjøres for visning`() {
        val fødselsnummer = lagFødselsnummer()
        vedtaksløsningenMottarNySøknad(fødselsnummer = fødselsnummer)
        spleisOppretterNyBehandling(fødselsnummer = fødselsnummer)
        håndterSkalKlargjøresForVisning(fødselsnummer = fødselsnummer)
        håndterPersoninfoløsning(fødselsnummer = fødselsnummer)
        assertSisteEtterspurteBehov("HentEnhet")
    }

    @Test
    fun `Henter egen ansatt-status når person skal klargjøres for visning`() {
        val fødselsnummer = lagFødselsnummer()
        vedtaksløsningenMottarNySøknad(fødselsnummer = fødselsnummer)
        spleisOppretterNyBehandling(fødselsnummer = fødselsnummer)
        håndterSkalKlargjøresForVisning(fødselsnummer = fødselsnummer)
        håndterPersoninfoløsning(fødselsnummer = fødselsnummer)
        håndterEnhetløsning(fødselsnummer = fødselsnummer)
        assertSisteEtterspurteBehov("EgenAnsatt")
    }

    @Test
    fun `Har nødvendige tilgangsdata når vi har klargjort person for visning`() {
        val fødselsnummer = lagFødselsnummer()
        vedtaksløsningenMottarNySøknad(fødselsnummer = fødselsnummer)
        spleisOppretterNyBehandling(fødselsnummer = fødselsnummer)
        håndterSkalKlargjøresForVisning(fødselsnummer = fødselsnummer)
        håndterPersoninfoløsning(fødselsnummer = fødselsnummer)
        håndterEnhetløsning(fødselsnummer = fødselsnummer)
        håndterEgenansattløsning(fødselsnummer = fødselsnummer)

        assertHarTilgangsdata(fødselsnummer)
        assertOpptegnelse(fødselsnummer, Opptegnelse.Type.PERSON_KLAR_TIL_BEHANDLING)
    }

    private fun assertOpptegnelse(fødselsnummer: String, opptegnelseType: Opptegnelse.Type) {
        val opptegnelser = sessionFactory.transactionalSessionScope { session ->
            session.opptegnelseRepository.finnAlleForPersonEtter(
                Sekvensnummer(0),
                Identitetsnummer.fraString(fødselsnummer)
            )
        }

        assertEquals(1, opptegnelser.size)
        assertEquals(opptegnelseType, opptegnelser.single().type)
    }

    private fun assertHarTilgangsdata(fødselsnummer: String) {
        assertEquals(
            true,
            sessionFactory.transactionalSessionScope {
                it.personRepository.finn(
                    Identitetsnummer.fraString(
                        fødselsnummer
                    )
                )?.harDataNødvendigForVisning()
            }
        )
    }
}
