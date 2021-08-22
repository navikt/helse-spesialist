package no.nav.helse.notat

import no.nav.helse.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class NotatDaoTest: DatabaseIntegrationTest() {

    @BeforeEach
    fun setup() {
        resetDatabase()
    }

    @Test
    fun `finner notater`() {
        //given
        val saksbehandler_oid = saksbehandler()
        val oppgave_id = 1
        val oppgave_id_2 = 2
        val tekst = "Banan eple kake"
        val tekst_2 = "Eple kake banan"

        //when
        notatDao.opprettNotat(oppgave_id, tekst, saksbehandler_oid)
        notatDao.opprettNotat(oppgave_id, tekst_2, saksbehandler_oid)
        notatDao.opprettNotat(oppgave_id_2, tekst, saksbehandler_oid)

        val notater = notatDao.finnNotater(oppgave_id)

        //then
        assertEquals(2, notater.size)

        assertEquals(notater[0].tekst, tekst)
        assertEquals(notater[0].saksbehandlerOid, saksbehandler_oid)
        assertEquals(notater[1].tekst, tekst_2)
    }

    @Test
    fun `lagre notat`() {
        val rowsAffected = notatDao.opprettNotat(1, "tekst", UUID.randomUUID())
        assertEquals(1, rowsAffected)
    }

}
