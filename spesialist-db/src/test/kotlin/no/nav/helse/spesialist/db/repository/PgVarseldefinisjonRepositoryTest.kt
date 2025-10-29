package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PgVarseldefinisjonRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = sessionContext.varseldefinisjonRepository

    @Test
    fun `finn gjeldende varseldefinisjon for gitt kode`() {
        // given
        val kode = "EN_KODE"
        val nyTittel = "Ny varseldefinisjon"
        opprettVarseldefinisjon(
            tittel = "Gammel varseldefinisjon",
            kode = kode
        )
        opprettVarseldefinisjon(
            tittel = nyTittel,
            kode = kode
        )

        // when
        val gjeldende = repository.finnGjeldendeFor(kode)

        // then
        assertNotNull(gjeldende)
        assertEquals(kode, gjeldende.kode)
        assertEquals(nyTittel, nyTittel)
    }
}
