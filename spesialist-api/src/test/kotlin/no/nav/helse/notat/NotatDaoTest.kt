package no.nav.helse.notat

import no.nav.helse.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class NotatDaoTest: DatabaseIntegrationTest() {

    @Test
    fun `finner flere notater tilhørende samme vedtaksperiode`() {
        //given
        val saksbehandler_oid = saksbehandler()
        nyVedtaksperiode()
        val vedtaksperiodeId = PERIODE.first
        val tekster = listOf("Banan eple kake", "Eple kake banan")

        //when
        notatDao.opprettNotat(vedtaksperiodeId, tekster[0], saksbehandler_oid)
        notatDao.opprettNotat(vedtaksperiodeId, tekster[1], saksbehandler_oid)

        val notater = notatDao.finnNotater(listOf(vedtaksperiodeId))

        //then
        assertEquals(1, notater.size)
        assertEquals(2, notater[vedtaksperiodeId]?.size)

        assertNotEquals(notater[vedtaksperiodeId]?.get(0)?.tekst, notater[vedtaksperiodeId]?.get(1)?.tekst)
        notater[vedtaksperiodeId]?.forEach { notat ->
            assertEquals(saksbehandler_oid, notat.saksbehandlerOid)
            assertTrue(tekster.contains(notat.tekst))
        }
    }

    @Test
    fun `lagre notat`() {
        nyVedtaksperiode()
        val oid = saksbehandler()
        val vedtaksperiodeId = PERIODE.first

        val rowsAffected = notatDao.opprettNotat(vedtaksperiodeId, "tekst", oid)

        assertEquals(1, rowsAffected)
    }

    @Test
    fun `feilregistrer notat`() {
        nyVedtaksperiode()
        val oid = saksbehandler()
        val vedtaksperiodeId = PERIODE.first

        notatDao.opprettNotat(vedtaksperiodeId, "tekst", oid)
        val notater = notatDao.finnNotater(listOf(vedtaksperiodeId))

        val notatId = notater[vedtaksperiodeId]?.get(0)!!.id
        notatDao.feilregistrer(notatId, oid)

        val feilregistrerteNotater = notatDao.finnNotater(listOf(vedtaksperiodeId))

        assertTrue(feilregistrerteNotater[vedtaksperiodeId]?.get(0)!!.feilregistrert)
        assertNotNull(feilregistrerteNotater[vedtaksperiodeId]?.get(0)!!.feilregistrert_tidspunkt)
    }

}
