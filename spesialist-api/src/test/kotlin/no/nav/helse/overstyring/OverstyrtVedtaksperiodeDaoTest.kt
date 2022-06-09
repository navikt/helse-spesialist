package no.nav.helse.overstyring


import java.util.UUID
import no.nav.helse.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

internal class OverstyrtVedtaksperiodeDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `Kan lagre og hente overstyrt vedtaksperiode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(vedtaksperiodeId)
        val erVedtaksperiodeOverstyrt = overstyrtVedtaksperiodeDao.erVedtaksperiodeOverstyrt(vedtaksperiodeId)
        assertTrue(erVedtaksperiodeOverstyrt)
    }

    @Test
    fun `Henter ut overstyrt vedtaksperiode`() {
        val erVedtaksperiodeOverstyrt = overstyrtVedtaksperiodeDao.erVedtaksperiodeOverstyrt(UUID.randomUUID())
        assertFalse(erVedtaksperiodeOverstyrt)
    }
}
