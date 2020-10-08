package no.nav.helse.modell.automatisering

import DatabaseIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

internal class AutomatiseringDaoTest : DatabaseIntegrationTest() {

    @BeforeEach
    fun setup() {
        testhendelse()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
    }

    @Test
    fun `lagre og lese false`() {
        val automatisert = false
        automatiseringDao.lagre(automatisert, listOf("Problem"), VEDTAKSPERIODE, HENDELSE_ID)
        val automatiseringSvar = requireNotNull(automatiseringDao.hentAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))

        assertEquals(automatisert, automatiseringSvar.automatisert)
        assertEquals(VEDTAKSPERIODE, automatiseringSvar.vedtaksperiodeId)
        assertEquals(HENDELSE_ID, automatiseringSvar.hendelseId)
        assertEquals(1, automatiseringSvar.problemer.size)
    }

    @Test
    fun `lagre og lese true`() {
        val automatisert = true
        automatiseringDao.lagre(automatisert, emptyList(), VEDTAKSPERIODE, HENDELSE_ID)
        val automatiseringSvar = requireNotNull(automatiseringDao.hentAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))

        assertEquals(automatisert, automatiseringSvar.automatisert)
        assertEquals(VEDTAKSPERIODE, automatiseringSvar.vedtaksperiodeId)
        assertEquals(HENDELSE_ID, automatiseringSvar.hendelseId)
        assertEquals(0, automatiseringSvar.problemer.size)
    }

    @Test
    fun `finner ikke automatisering`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        assertNull(automatiseringDao.hentAutomatisering(vedtaksperiodeId, hendelseId))
    }

    @Test
    fun `lagre to automatiseringer på samme vedtaksperiode`() {
        val hendelseId2 = UUID.randomUUID()
        testhendelse(hendelseId = hendelseId2)

        automatiseringDao.lagre(false, listOf("problem"), VEDTAKSPERIODE, HENDELSE_ID)
        automatiseringDao.lagre(true, emptyList(), VEDTAKSPERIODE, hendelseId2)

        val automatiseringSvar1 = requireNotNull(automatiseringDao.hentAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))
        val automatiseringSvar2 = requireNotNull(automatiseringDao.hentAutomatisering(VEDTAKSPERIODE, hendelseId2))

        assertEquals(false, automatiseringSvar1.automatisert)
        assertEquals(1, automatiseringSvar1.problemer.size)
        assertEquals(true, automatiseringSvar2.automatisert)
        assertEquals(0, automatiseringSvar2.problemer.size)
    }

    @Test
    fun `to automatiseringer på samme vedtaksperiode og samme hendelseID kræsjer`() {
        automatiseringDao.lagre(false, listOf("problem"), VEDTAKSPERIODE, HENDELSE_ID)
        assertFails { automatiseringDao.lagre(true, emptyList(), VEDTAKSPERIODE, HENDELSE_ID) }

        val automatiseringSvar = requireNotNull(automatiseringDao.hentAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))

        assertEquals(false, automatiseringSvar.automatisert)
        assertEquals(VEDTAKSPERIODE, automatiseringSvar.vedtaksperiodeId)
        assertEquals(HENDELSE_ID, automatiseringSvar.hendelseId)
        assertEquals(1, automatiseringSvar.problemer.size)
    }
}
