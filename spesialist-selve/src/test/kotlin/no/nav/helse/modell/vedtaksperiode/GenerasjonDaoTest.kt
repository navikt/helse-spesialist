package no.nav.helse.modell.vedtaksperiode

import DatabaseIntegrationTest
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GenerasjonDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `finner generasjon for vedtaksperiode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val generasjon = generasjonDao.prøvOpprett(vedtaksperiodeId, vedtaksperiodeEndretId)

        assertEquals(1L, generasjon)
    }

    @Test
    fun `oppretter ikke generasjon, så lenge det finnes åpen`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val spørring1 = generasjonDao.prøvOpprett(vedtaksperiodeId, vedtaksperiodeEndretId)
        val spørring2 = generasjonDao.prøvOpprett(vedtaksperiodeId, vedtaksperiodeEndretId)

        assertEquals(1L, spørring1)
        assertEquals(null, spørring2)
    }

    @Test
    fun `får ny generasjon når forrige er låst`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val vedtakFattetId = UUID.randomUUID()
        val spørring1 = generasjonDao.prøvOpprett(vedtaksperiodeId, vedtaksperiodeEndretId)
        generasjonDao.låsGenerasjon(vedtaksperiodeId, vedtakFattetId)
        val spørring2 = generasjonDao.prøvOpprett(vedtaksperiodeId, vedtaksperiodeEndretId)

        assertEquals(1L, spørring1)
        assertEquals(2L, spørring2)
    }

    @Test
    fun `sjekker at lås returnerer iden til raden`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val vedtakFattetId = UUID.randomUUID()
        val spørring1 = generasjonDao.prøvOpprett(vedtaksperiodeId, vedtaksperiodeEndretId)
        val låstGenerasjon = generasjonDao.låsGenerasjon(vedtaksperiodeId, vedtakFattetId)

        assertEquals(1L, spørring1)
        assertEquals(spørring1, låstGenerasjon)
    }

    @Test
    fun `sjekker at siste generasjon blir returnert`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val vedtakFattetId = UUID.randomUUID()
        generasjonDao.prøvOpprett(vedtaksperiodeId, vedtaksperiodeEndretId)
        generasjonDao.låsGenerasjon(vedtaksperiodeId, vedtakFattetId)
        val spørring2 = generasjonDao.prøvOpprett(vedtaksperiodeId, vedtaksperiodeEndretId)
        val gjeldendeGenerasjon = generasjonDao.generasjon(vedtaksperiodeId)

        assertEquals(gjeldendeGenerasjon, spørring2)
    }
}