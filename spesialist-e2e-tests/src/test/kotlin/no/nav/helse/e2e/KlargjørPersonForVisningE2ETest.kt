package no.nav.helse.e2e

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.spesialist.test.lagFødselsnummer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class KlargjørPersonForVisningE2ETest : AbstractE2ETest() {

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
        assertOpptegnelse(fødselsnummer, OpptegnelseDao.Opptegnelse.Type.PERSON_KLAR_TIL_BEHANDLING)
    }

    @Test
    fun `Feilende test - skal kunne vise personen når tilgangsdata er hentet inn`() {
        val fødselsnummer = lagFødselsnummer()

        vedtaksløsningenMottarNySøknad(fødselsnummer = fødselsnummer)
        spleisOppretterNyBehandling(fødselsnummer = fødselsnummer)
        håndterSkalKlargjøresForVisning(fødselsnummer = fødselsnummer)
        håndterPersoninfoløsning(fødselsnummer = fødselsnummer)
        håndterEnhetløsning(fødselsnummer = fødselsnummer)
        håndterEgenansattløsning(fødselsnummer = fødselsnummer)

        assertKanVisePersonen(fødselsnummer)
    }

    private fun assertOpptegnelse(fødselsnummer: String, opptegnelseType: OpptegnelseDao.Opptegnelse.Type) {
        val opptegnelser = sessionOf(dataSource).use {
            @Language("PostgreSQL")
            val query = """SELECT type FROM opptegnelse WHERE person_id = (SELECT id FROM person WHERE fødselsnummer = ?)"""
            it.run(queryOf(query, fødselsnummer).map { row -> enumValueOf<OpptegnelseDao.Opptegnelse.Type>(row.string("type")) }.asList)
        }

        assertEquals(1, opptegnelser.size)
        assertEquals(opptegnelseType, opptegnelser.single())
    }

    private fun assertHarTilgangsdata(fødselsnummer: String) {
        val dao = daos.personApiDao
        assertTrue(dao.harDataNødvendigForVisning(fødselsnummer))
    }

    private fun assertKanVisePersonen(fødselsnummer: String) {
        val dao = daos.personApiDao
        assertTrue(dao.harDataNødvendigForVisning(fødselsnummer))
    }
}
