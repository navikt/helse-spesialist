package no.nav.helse.spesialist.api.notat

import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NotatDaoTest: DatabaseIntegrationTest() {

    @Test
    fun `finner flere notater tilhørende samme vedtaksperiode`() {
        //given
        val saksbehandler_oid = opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
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
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oid = opprettSaksbehandler()
        val vedtaksperiodeId = PERIODE.id

        notatDao.opprettNotat(vedtaksperiodeId, "tekst", oid)

        val notater = notatDao.finnNotater(listOf(vedtaksperiodeId))

        assertEquals(NotatType.Generelt, notater[vedtaksperiodeId]?.get(0)?.type)
    }

    @Test
    fun `lagre notat`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oid = opprettSaksbehandler()
        val vedtaksperiodeId = PERIODE.id

        val tekst = "tekst"
        val notatDto = notatDao.opprettNotat(vedtaksperiodeId, tekst, oid)
        assertEquals(tekst, notatDto?.tekst)
        assertEquals(vedtaksperiodeId, notatDto?.vedtaksperiodeId)
        assertEquals(oid, notatDto?.saksbehandlerOid)
        assertEquals(SAKSBEHANDLER.epost, notatDto?.saksbehandlerEpost)
        assertEquals(SAKSBEHANDLER.navn, notatDto?.saksbehandlerNavn)
        assertEquals(SAKSBEHANDLER.ident, notatDto?.saksbehandlerIdent)
        assertEquals(NotatType.Generelt, notatDto?.type)
    }

    @Test
    fun `lagre påvent-notat`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oid = opprettSaksbehandler()
        val vedtaksperiodeId = PERIODE.id

        notatDao.opprettNotat(vedtaksperiodeId, "tekst", oid, NotatType.PaaVent)

        val notater = notatDao.finnNotater(listOf(vedtaksperiodeId))

        assertEquals(NotatType.PaaVent, notater[vedtaksperiodeId]?.get(0)?.type) }

    @Test
    fun `feilregistrer notat`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
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
