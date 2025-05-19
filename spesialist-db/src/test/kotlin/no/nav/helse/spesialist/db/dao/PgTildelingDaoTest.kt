package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertNotNull

internal class PgTildelingDaoTest : AbstractDBIntegrationTest() {

    @Test
    fun `henter saksbehandlerepost for tildeling med fødselsnummer`() {
        val saksbehandler = nyLegacySaksbehandler()
        val fødselsnummer = lagFødselsnummer()
        nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)
            .tildelOgLagre(saksbehandler)

        val tildeling = tildelingDao.tildelingForPerson(fødselsnummer)
        assertEquals(saksbehandler.epostadresse, tildeling?.epost)
    }

    @Test
    fun `finn tildeling for oppgave`() {
        val saksbehandler = nyLegacySaksbehandler()
        val fødselsnummer = lagFødselsnummer()
        val oppgave = nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)
            .tildelOgLagre(saksbehandler)
        val tildeling = tildelingDao.tildelingForOppgave(oppgave.id)
        assertNotNull(tildeling)
        assertEquals(saksbehandler.oid, tildeling.oid)
        assertEquals(saksbehandler.epostadresse, tildeling.epost)
        assertEquals(saksbehandler.navn, tildeling.navn)
    }

    @Test
    fun `henter nyeste tildeling for person`() {
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiodeId = UUID.randomUUID()
        val saksbehandler1 = nyLegacySaksbehandler()
        val saksbehandler2 = nyLegacySaksbehandler()
        nyOppgaveForNyPerson(fødselsnummer = fødselsnummer, vedtaksperiodeId = vedtaksperiodeId)
            .tildelOgLagre(saksbehandler1)
            .invaliderOgLagre()

        opprettOppgave(vedtaksperiodeId = vedtaksperiodeId).tildelOgLagre(saksbehandler2)
        val tildeling = tildelingDao.tildelingForPerson(fødselsnummer)
        assertNotNull(tildeling)
        assertEquals(saksbehandler2.oid, tildeling.oid)
        assertEquals(saksbehandler2.epostadresse, tildeling.epost)
        assertEquals(saksbehandler2.navn, tildeling.navn)
    }

    @Test
    fun `henter nyeste tildeling for person selvom oppgaven er invalidert`() {
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiodeId = UUID.randomUUID()
        val saksbehandler1 = nyLegacySaksbehandler()
        val saksbehandler2 = nyLegacySaksbehandler()
        nyOppgaveForNyPerson(fødselsnummer = fødselsnummer, vedtaksperiodeId = vedtaksperiodeId)
            .tildelOgLagre(saksbehandler1)
            .invaliderOgLagre()

        opprettOppgave(vedtaksperiodeId = vedtaksperiodeId)
            .tildelOgLagre(saksbehandler2)
            .invaliderOgLagre()
        val tildeling = tildelingDao.tildelingForPerson(fødselsnummer)
        assertNotNull(tildeling)
        assertEquals(saksbehandler2.oid, tildeling.oid)
        assertEquals(saksbehandler2.epostadresse, tildeling.epost)
        assertEquals(saksbehandler2.navn, tildeling.navn)
    }

    @Test
    fun `finner ikke tildeling for oppgave som ikke er tildelt`() {
        val fødselsnummer = lagFødselsnummer()
        nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)
            .invaliderOgLagre()
        val tildeling = tildelingDao.tildelingForPerson(fødselsnummer)
        assertNull(tildeling)
    }
}
