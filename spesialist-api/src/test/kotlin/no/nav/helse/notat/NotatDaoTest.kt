package no.nav.helse.notat

import no.nav.helse.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class NotatDaoTest: DatabaseIntegrationTest() {

    @BeforeEach
    fun setup() {
        resetDatabase()
    }

    @Test
    fun `finner flere notater tilhørende samme oppgave`() {
        //given
        val saksbehandler_oid = saksbehandler()
        val oppgave_id = 1
        val tekster = listOf("Banan eple kake", "Eple kake banan")

        //when
        notatDao.opprettNotat(oppgave_id, tekster[0], saksbehandler_oid)
        notatDao.opprettNotat(oppgave_id, tekster[1], saksbehandler_oid)

        val notater = notatDao.finnNotater(listOf(oppgave_id))

        //then
        assertEquals(1, notater.size)
        assertEquals(2, notater[oppgave_id]?.size)

        assertNotEquals(notater[oppgave_id]?.get(0)?.tekst, notater[oppgave_id]?.get(1)?.tekst)
        notater[oppgave_id]?.forEach { notat ->
            assertEquals(saksbehandler_oid, notat.saksbehandlerOid)
            assertTrue(tekster.contains(notat.tekst))
        }
    }

    @Test
    fun `lagre notat`() {
        val rowsAffected = notatDao.opprettNotat(1, "tekst", UUID.randomUUID())
        assertEquals(1, rowsAffected)
    }

}
