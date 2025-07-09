package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.time.LocalDateTime
import java.util.UUID

@Isolated
class PgSaksbehandlerDaoTest : AbstractDBIntegrationTest() {

    @BeforeEach
    fun tømTabeller() {
        dbQuery.execute("TRUNCATE saksbehandler CASCADE ")
    }

    @Test
    fun `henter saksbehandler for ident`() {
        val ident = "L123456"
        opprettSaksbehandler(ident = ident)

        val resultat = saksbehandlerDao.hent(ident)
        assertNotNull(resultat)
        assertEquals(ident, resultat?.ident)
    }

    @Test
    fun `ukjent saksbehandler-ident retunerer null`() {
        opprettSaksbehandler(ident = "L112233")
        val resultat = saksbehandlerDao.hent("E654321")
        assertNull(resultat)
    }

    @Test
    fun `henter alle saksbehandlere som har vært aktiv siste tre mnd`() {
        val saksbehandlerSistAktivFireMnderSiden = opprettSaksbehandler(
            saksbehandlerOID = UUID.randomUUID(),
            ident = "T112233"
        )
        saksbehandlerDao.oppdaterSistObservert(
            saksbehandlerSistAktivFireMnderSiden,
            LocalDateTime.now().minusMonths(4)
        )
        listOf("T445566", "T778899").forEach {
            val id = opprettSaksbehandler(
                saksbehandlerOID = UUID.randomUUID(),
                ident = it
            )
            saksbehandlerDao.oppdaterSistObservert(
                id,
                LocalDateTime.now().minusMonths(1)
            )
        }

        val aktiveSaksbehandlereSisteTreMnd = saksbehandlerDao.hentAlleAktiveSisteTreMnder()
        assertEquals(2, aktiveSaksbehandlereSisteTreMnd.size)
    }
}
