package no.nav.helse.modell.vedtaksperiode

import DatabaseIntegrationTest
import java.util.UUID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GenerasjonDaoTest : DatabaseIntegrationTest() {

    private companion object {
        private val VEDTAKSPERIODE_ENDRET_HENDELSE_ID = UUID.randomUUID()
        private val VEDTAK_FATTET_HENDELSE_ID = UUID.randomUUID()
    }

    @Test
    fun `oppretter generasjon for vedtaksperiode`() {
        val generasjon = generasjonDao.prøvOpprett(VEDTAKSPERIODE_ID, VEDTAKSPERIODE_ENDRET_HENDELSE_ID)

        assertEquals(1L, generasjon)
    }

    @Test
    fun `oppretter ikke generasjon, så lenge det finnes en ulåst generasjon`() {
        val førsteGenerasjon = generasjonDao.prøvOpprett(VEDTAKSPERIODE_ID, VEDTAKSPERIODE_ENDRET_HENDELSE_ID)
        val andreGenerasjonBurdeIkkeOpprettes =
            generasjonDao.prøvOpprett(VEDTAKSPERIODE_ID, VEDTAKSPERIODE_ENDRET_HENDELSE_ID)

        assertEquals(1L, førsteGenerasjon)
        assertEquals(null, andreGenerasjonBurdeIkkeOpprettes)
    }

    @Test
    fun `oppretter ny generasjon når forrige er låst`() {
        val førsteGenerasjon = generasjonDao.prøvOpprett(VEDTAKSPERIODE_ID, VEDTAKSPERIODE_ENDRET_HENDELSE_ID)
        generasjonDao.låsGenerasjon(VEDTAKSPERIODE_ID, VEDTAK_FATTET_HENDELSE_ID)
        val andreGenerasjon = generasjonDao.prøvOpprett(VEDTAKSPERIODE_ID, VEDTAKSPERIODE_ENDRET_HENDELSE_ID)

        assertEquals(1L, førsteGenerasjon)
        assertEquals(2L, andreGenerasjon)
    }

    @Test
    fun `sjekker at låsGenerasjon returnerer id'en til raden`() {
        val førsteGenerasjon = generasjonDao.prøvOpprett(VEDTAKSPERIODE_ID, VEDTAKSPERIODE_ENDRET_HENDELSE_ID)
        val låstGenerasjon = generasjonDao.låsGenerasjon(VEDTAKSPERIODE_ID, VEDTAK_FATTET_HENDELSE_ID)

        assertEquals(1L, førsteGenerasjon)
        assertEquals(førsteGenerasjon, låstGenerasjon)
    }

    @Test
    fun `sjekker at siste generasjon blir returnert`() {
        generasjonDao.prøvOpprett(VEDTAKSPERIODE_ID, VEDTAKSPERIODE_ENDRET_HENDELSE_ID)
        generasjonDao.låsGenerasjon(VEDTAKSPERIODE_ID, VEDTAK_FATTET_HENDELSE_ID)
        val andreGenerasjon = generasjonDao.prøvOpprett(VEDTAKSPERIODE_ID, VEDTAKSPERIODE_ENDRET_HENDELSE_ID)
        val gjeldendeGenerasjon = generasjonDao.generasjon(VEDTAKSPERIODE_ID)

        assertEquals(gjeldendeGenerasjon, andreGenerasjon)
    }
}