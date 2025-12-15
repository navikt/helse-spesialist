package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.time.LocalDateTime

@Isolated
class PgSaksbehandlerDaoTest : AbstractDBIntegrationTest() {
    @BeforeEach
    fun tømTabeller() {
        dbQuery.execute("TRUNCATE saksbehandler CASCADE ")
    }

    @Test
    fun `henter saksbehandler for ident`() {
        val ident = lagSaksbehandler().ident
        opprettSaksbehandler(ident = ident)

        val resultat = saksbehandlerDao.hent(ident.value)
        assertNotNull(resultat)
        assertEquals(ident, resultat?.ident)
    }

    @Test
    fun `ukjent saksbehandler-ident retunerer null`() {
        opprettSaksbehandler(ident = lagSaksbehandler().ident)
        val resultat = saksbehandlerDao.hent("E654321")
        assertNull(resultat)
    }

    @Test
    fun `henter alle saksbehandlere som har vært aktiv siste tre mnd`() {
        val saksbehandlerSistAktivFireMnderSiden =
            lagSaksbehandler().also {
                opprettSaksbehandler(it.id.value, it.navn, it.epost, it.ident)
            }
        saksbehandlerDao.oppdaterSistObservert(
            saksbehandlerSistAktivFireMnderSiden.id.value,
            LocalDateTime.now().minusMonths(4),
        )
        listOf("T445566", "T778899").forEach {
            val saksbehandler = lagSaksbehandler(ident = it)
            val id =
                opprettSaksbehandler(saksbehandler.id.value, saksbehandler.navn, saksbehandler.epost, saksbehandler.ident)

            saksbehandlerDao.oppdaterSistObservert(
                id,
                LocalDateTime.now().minusMonths(1),
            )
        }

        val aktiveSaksbehandlereSisteTreMnd = saksbehandlerDao.hentAlleAktiveSisteTreMnderEllerHarTildelteOppgaver()
        assertEquals(2, aktiveSaksbehandlereSisteTreMnd.size)
    }

    @Test
    fun `henter alle saksbehandlere som har vært inaktiv mer enn tre mnd og har tildelte oppgaver`() {
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = nyLegacySaksbehandler()
        val saksbehandler2 = nyLegacySaksbehandler()

        saksbehandlerDao.oppdaterSistObservert(
            saksbehandler.saksbehandler.id.value,
            LocalDateTime.now().minusMonths(4),
        )

        saksbehandlerDao.oppdaterSistObservert(
            saksbehandler2.saksbehandler.id.value,
            LocalDateTime.now().minusMonths(4),
        )

        nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)
            .tildelOgLagre(saksbehandler)
            .avventSystemOgLagre(saksbehandler)
            .ferdigstillOgLagre()

        val aktiveSaksbehandlereSisteTreMnd = saksbehandlerDao.hentAlleAktiveSisteTreMnderEllerHarTildelteOppgaver()
        assertEquals(1, aktiveSaksbehandlereSisteTreMnd.size)
    }
}
