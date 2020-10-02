package no.nav.helse.modell.automatisering

import DatabaseIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
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
        automatiseringDao.lagre(automatisert, VEDTAKSPERIODE, HENDELSE_ID)
        val automatiseringSvar = requireNotNull(automatiseringDao.hentAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))

        assertEquals(automatisert, automatiseringSvar.automatisert)
        assertEquals(VEDTAKSPERIODE, automatiseringSvar.vedtaksperiodeId)
        assertEquals(HENDELSE_ID, automatiseringSvar.hendelseId)
    }

    @Test
    fun `lagre og lese true`() {
        val automatisert = true
        automatiseringDao.lagre(automatisert, VEDTAKSPERIODE, HENDELSE_ID)
        val automatiseringSvar = requireNotNull(automatiseringDao.hentAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))

        assertEquals(automatisert, automatiseringSvar.automatisert)
        assertEquals(VEDTAKSPERIODE, automatiseringSvar.vedtaksperiodeId)
        assertEquals(HENDELSE_ID, automatiseringSvar.hendelseId)
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

        automatiseringDao.lagre(false, VEDTAKSPERIODE, HENDELSE_ID)
        automatiseringDao.lagre(true, VEDTAKSPERIODE, hendelseId2)

        val automatiseringSvar1 = requireNotNull(automatiseringDao.hentAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))
        val automatiseringSvar2 = requireNotNull(automatiseringDao.hentAutomatisering(VEDTAKSPERIODE, hendelseId2))

        assertEquals(false, automatiseringSvar1.automatisert)
        assertEquals(true, automatiseringSvar2.automatisert)
    }

    @Test
    fun `lagre to automatiseringer på samme vedtaksperiode og samme hendelseID`() {
        automatiseringDao.lagre(false, VEDTAKSPERIODE, HENDELSE_ID)
        automatiseringDao.lagre(true, VEDTAKSPERIODE, HENDELSE_ID)

        val automatiseringSvar = requireNotNull(automatiseringDao.hentAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))

        assertEquals(true, automatiseringSvar.automatisert)
        assertEquals(VEDTAKSPERIODE, automatiseringSvar.vedtaksperiodeId)
        assertEquals(HENDELSE_ID, automatiseringSvar.hendelseId)
    }
}
