package no.nav.helse.spesialist.api.notat

import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class NotatDaoTest: DatabaseIntegrationTest() {

    @Test
    fun `finner flere notater tilhørende samme vedtaksperiode`() {
        //given
        val saksbehandler_oid = opprettSaksbehandler()
        opprettVedtaksperiode()
        val vedtaksperiodeId = PERIODE.id
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
    fun `notater defaulter til type Generelt`() {
        opprettVedtaksperiode()
        val oid = opprettSaksbehandler()
        val vedtaksperiodeId = PERIODE.id

        notatDao.opprettNotat(vedtaksperiodeId, "tekst", oid)

        val notater = notatDao.finnNotater(listOf(vedtaksperiodeId))

        assertEquals(NotatType.Generelt, notater[vedtaksperiodeId]?.get(0)?.type)
    }

    @Test
    fun `lagre notat`() {
        opprettVedtaksperiode()
        val oid = opprettSaksbehandler()
        val vedtaksperiodeId = PERIODE.id

        val rowsAffected = notatDao.opprettNotat(vedtaksperiodeId, "tekst", oid)

        assertEquals(1, rowsAffected)
    }

    @Test
    fun `lagre påvent-notat`() {
        opprettVedtaksperiode()
        val oid = opprettSaksbehandler()
        val vedtaksperiodeId = PERIODE.id

        notatDao.opprettNotat(vedtaksperiodeId, "tekst", oid, NotatType.PaaVent)

        val notater = notatDao.finnNotater(listOf(vedtaksperiodeId))

        assertEquals(NotatType.PaaVent, notater[vedtaksperiodeId]?.get(0)?.type) }

    @Test
    fun `feilregistrer notat`() {
        opprettVedtaksperiode()
        val oid = opprettSaksbehandler()
        val vedtaksperiodeId = PERIODE.id

        notatDao.opprettNotat(vedtaksperiodeId, "tekst", oid)
        val notater = notatDao.finnNotater(listOf(vedtaksperiodeId))

        val notatId = notater[vedtaksperiodeId]?.get(0)!!.id
        notatDao.feilregistrerNotat(notatId)

        val feilregistrerteNotater = notatDao.finnNotater(listOf(vedtaksperiodeId))

        assertTrue(feilregistrerteNotater[vedtaksperiodeId]?.get(0)!!.feilregistrert)
        assertNotNull(feilregistrerteNotater[vedtaksperiodeId]?.get(0)!!.feilregistrert_tidspunkt)
    }

}
