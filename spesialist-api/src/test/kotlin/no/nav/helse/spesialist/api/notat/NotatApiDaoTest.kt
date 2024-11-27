package no.nav.helse.spesialist.api.notat

import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NotatApiDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `notater defaulter til type Generelt`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oid = opprettSaksbehandler()
        val vedtaksperiodeId = PERIODE.id

        notatDao.opprettNotat(vedtaksperiodeId, "tekst", oid)

        val notater = notatDao.finnNotater(vedtaksperiodeId)

        assertEquals(NotatType.Generelt, notater[0].type)
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
    fun `lagre kommentar`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oid = opprettSaksbehandler()
        val vedtaksperiodeId = PERIODE.id

        val tekst = "tekst"
        val notatDto = notatDao.opprettNotat(vedtaksperiodeId, tekst, oid)
        checkNotNull(notatDto)
        val kommentarDto = notatDao.leggTilKommentar(notatDto.dialogRef, tekst, SAKSBEHANDLER.ident)
        assertEquals(tekst, kommentarDto?.tekst)
    }

    @Test
    fun `feilregistrer notat`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oid = opprettSaksbehandler()
        val vedtaksperiodeId = PERIODE.id

        notatDao.opprettNotat(vedtaksperiodeId, "tekst", oid)
        val notater = notatDao.finnNotater(vedtaksperiodeId)

        val notatId = notater[0].id
        notatDao.feilregistrerNotat(notatId)

        val feilregistrerteNotater = notatDao.finnNotater(vedtaksperiodeId)

        assertTrue(feilregistrerteNotater[0].feilregistrert)
        assertNotNull(feilregistrerteNotater[0].feilregistrert_tidspunkt)
    }
}
