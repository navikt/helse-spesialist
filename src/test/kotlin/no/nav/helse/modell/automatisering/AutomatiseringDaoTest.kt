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
}
