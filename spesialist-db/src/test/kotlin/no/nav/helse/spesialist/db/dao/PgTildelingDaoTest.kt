package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

internal class PgTildelingDaoTest : AbstractDBIntegrationTest() {

    @Test
    fun `henter saksbehandlerepost for tildeling med fødselsnummer`() {
        val saksbehandler = nyLegacySaksbehandler()
        val fødselsnummer = lagFødselsnummer()
        nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)
            .tildelOgLagre(saksbehandler)

        val tildeling = tildelingDao.tildelingForPerson(fødselsnummer)
        assertEquals(saksbehandler.saksbehandler.epost, tildeling?.epost)
    }

    @Test
    fun `finn tildeling for oppgave`() {
        val saksbehandler = nyLegacySaksbehandler()
        val fødselsnummer = lagFødselsnummer()
        val oppgave = nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)
            .tildelOgLagre(saksbehandler)
        val tildeling = tildelingDao.tildelingForOppgave(oppgave.id)
        assertNotNull(tildeling)
        assertEquals(saksbehandler.saksbehandler.id().value, tildeling.oid)
        assertEquals(saksbehandler.saksbehandler.epost, tildeling.epost)
        assertEquals(saksbehandler.saksbehandler.navn, tildeling.navn)
    }

    @Test
    fun `henter bare tildelinger som har en aktiv oppgave`() {
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = nyLegacySaksbehandler()
        nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)
            .tildelOgLagre(saksbehandler)
            .avventSystemOgLagre(saksbehandler)
            .ferdigstillOgLagre()
        val saksbehandlerepost = tildelingDao.tildelingForPerson(fødselsnummer)
        assertNull(saksbehandlerepost)
    }
}
