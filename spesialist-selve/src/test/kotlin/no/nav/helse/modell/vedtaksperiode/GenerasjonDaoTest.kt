package no.nav.helse.modell.vedtaksperiode

import DatabaseIntegrationTest
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GenerasjonDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `finner generasjon for vedtaksperiode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjonDao.finnUlåstEllerOpprett(vedtaksperiodeId)

        assertEquals(1L, generasjon)
    }

    @Test
    fun `finner samme generasjon, så lenge den er ulåst`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spørring1 = generasjonDao.finnUlåstEllerOpprett(vedtaksperiodeId)
        val spørring2 = generasjonDao.finnUlåstEllerOpprett(vedtaksperiodeId)

        assertEquals(1L, spørring1)
        assertEquals(spørring1, spørring2)
    }

    @Test
    fun `får ny generasjon når forrige er låst`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spørring1 = generasjonDao.finnUlåstEllerOpprett(vedtaksperiodeId)
        generasjonDao.lås(vedtaksperiodeId)
        val spørring2 = generasjonDao.finnUlåstEllerOpprett(vedtaksperiodeId)

        assertEquals(1L, spørring1)
        assertEquals(2L, spørring2)
    }

    @Test
    fun `sjekker at lås returnerer iden til raden`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spørring1 = generasjonDao.finnUlåstEllerOpprett(vedtaksperiodeId)
        val låstGenerasjon = generasjonDao.lås(vedtaksperiodeId)

        assertEquals(1L, spørring1)
        assertEquals(spørring1, låstGenerasjon)
    }
}