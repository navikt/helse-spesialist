package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.test.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class KlargjørPersonForVisningE2ETest : AbstractE2ETest() {

    @Test
    fun `Har nødvendige tilgangsdata når vi har klargjort person for visning`() {
        val fødselsnummer = lagFødselsnummer()
        vedtaksløsningenMottarNySøknad(fødselsnummer = fødselsnummer)
        spleisOppretterNyBehandling(fødselsnummer = fødselsnummer)
        håndterSkalKlargjøresForVisning(fødselsnummer = fødselsnummer)
        håndterPersoninfoløsning(fødselsnummer = fødselsnummer)
        håndterEgenansattløsning(fødselsnummer = fødselsnummer)

        assertHarTilgangsdata(fødselsnummer)
        // sjekk at alt er som det skal være
    }

    @Test
    fun `Feilende test - skal kunne vise personen når tilgangsdata er hentet inn`() {
        val fødselsnummer = lagFødselsnummer()

        vedtaksløsningenMottarNySøknad(fødselsnummer = fødselsnummer)
        spleisOppretterNyBehandling(fødselsnummer = fødselsnummer)
        håndterSkalKlargjøresForVisning(fødselsnummer = fødselsnummer)
        håndterPersoninfoløsning(fødselsnummer = fødselsnummer)
        håndterEgenansattløsning(fødselsnummer = fødselsnummer)

        assertKanVisePersonen(fødselsnummer)
        // sjekk at alt er som det skal være
    }

    private fun assertHarTilgangsdata(fødselsnummer: String) {
        val dao = PersonApiDao(dataSource)
        assertTrue(dao.harTilgangsdata(fødselsnummer))
    }

    private fun assertKanVisePersonen(fødselsnummer: String) {
        val dao = PersonApiDao(dataSource)
        assertTrue(dao.harTilgangsdata(fødselsnummer))
    }
}
