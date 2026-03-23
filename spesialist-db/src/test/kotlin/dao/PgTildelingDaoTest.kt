package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class PgTildelingDaoTest : AbstractDBIntegrationTest() {
    @Test
    fun `henter saksbehandlerepost for tildeling med fødselsnummer`() {
        val saksbehandler = lagSaksbehandler()
        val fødselsnummer = lagFødselsnummer()
        nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)
            .tildelOgLagre(saksbehandler)

        val tildeling = tildelingDao.tildelingForPerson(fødselsnummer)
        assertEquals(saksbehandler.epost, tildeling?.epost)
    }

    @Test
    fun `henter bare tildelinger som har en aktiv oppgave`() {
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()
        nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)
            .tildelOgLagre(saksbehandler)
            .avventSystemOgLagre(saksbehandler)
            .ferdigstillOgLagre()
        val saksbehandlerepost = tildelingDao.tildelingForPerson(fødselsnummer)
        assertNull(saksbehandlerepost)
    }
}
