package no.nav.helse.db.api

import no.nav.helse.DatabaseIntegrationTest
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PgNotatApiDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `notater defaulter til type Generelt`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        val oid = opprettSaksbehandler()

        notatApiDao.opprettNotat(VEDTAKSPERIODE, "tekst", oid)

        val notater = notatApiDao.finnNotater(VEDTAKSPERIODE)

        assertEquals(NotatType.Generelt, notater[0].type)
    }

    @Test
    fun `lagre notat`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        val oid = opprettSaksbehandler()

        val tekst = "tekst"
        val notatDto = notatApiDao.opprettNotat(VEDTAKSPERIODE, tekst, oid)
        assertEquals(tekst, notatDto?.tekst)
        assertEquals(VEDTAKSPERIODE, notatDto?.vedtaksperiodeId)
        assertEquals(oid, notatDto?.saksbehandlerOid)
        assertEquals(SAKSBEHANDLER.epost, notatDto?.saksbehandlerEpost)
        assertEquals(SAKSBEHANDLER.navn, notatDto?.saksbehandlerNavn)
        assertEquals(SAKSBEHANDLER.ident, notatDto?.saksbehandlerIdent)
        assertEquals(NotatType.Generelt, notatDto?.type)
    }

    @Test
    fun `lagre kommentar`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        val oid = opprettSaksbehandler()

        val tekst = "tekst"
        val notatDto = notatApiDao.opprettNotat(VEDTAKSPERIODE, tekst, oid)
        checkNotNull(notatDto)
        val kommentarDto = notatApiDao.leggTilKommentar(notatDto.dialogRef, tekst, SAKSBEHANDLER.ident)
        assertEquals(tekst, kommentarDto?.tekst)
    }

    @Test
    fun `feilregistrer notat`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        val oid = opprettSaksbehandler()

        notatApiDao.opprettNotat(VEDTAKSPERIODE, "tekst", oid)
        val notater = notatApiDao.finnNotater(VEDTAKSPERIODE)

        val notatId = notater[0].id
        notatApiDao.feilregistrerNotat(notatId)

        val feilregistrerteNotater = notatApiDao.finnNotater(VEDTAKSPERIODE)

        assertTrue(feilregistrerteNotater[0].feilregistrert)
        assertNotNull(feilregistrerteNotater[0].feilregistrert_tidspunkt)
    }
}
