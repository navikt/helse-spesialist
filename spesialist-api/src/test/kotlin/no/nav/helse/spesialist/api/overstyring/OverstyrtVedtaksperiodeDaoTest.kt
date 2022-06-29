package no.nav.helse.spesialist.api.overstyring


import java.util.UUID
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OverstyrtVedtaksperiodeDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `Kan lagre og hente overstyrt vedtaksperiode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(vedtaksperiodeId, OverstyringType.Inntekt)
        overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(vedtaksperiodeId, OverstyringType.Dager)
        overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(vedtaksperiodeId, OverstyringType.Arbeidsforhold)

        val vedtaksperiodeOverstyrtTyper = overstyrtVedtaksperiodeDao.hentVedtaksperiodeOverstyrtTyper(vedtaksperiodeId)

        assertTrue(vedtaksperiodeOverstyrtTyper.isNotEmpty())
        assertEquals(3, vedtaksperiodeOverstyrtTyper.size)
        assertTrue(vedtaksperiodeOverstyrtTyper.contains(OverstyringType.Inntekt))
        assertTrue(vedtaksperiodeOverstyrtTyper.contains(OverstyringType.Dager))
        assertTrue(vedtaksperiodeOverstyrtTyper.contains(OverstyringType.Arbeidsforhold))
    }

    @Test
    fun `Henter ut overstyrt vedtaksperiode`() {
        val vedtaksperiodeOverstyrtTyper = overstyrtVedtaksperiodeDao.hentVedtaksperiodeOverstyrtTyper(UUID.randomUUID())
        assertTrue(vedtaksperiodeOverstyrtTyper.isEmpty())
    }
}
