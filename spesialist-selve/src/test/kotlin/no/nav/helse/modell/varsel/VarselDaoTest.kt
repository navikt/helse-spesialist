package no.nav.helse.modell.varsel

import DatabaseIntegrationTest
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VarselDaoTest: DatabaseIntegrationTest() {

    @Test
    fun `lagrer unike varsler`() {
        val dao = VarselDao(dataSource)
        val vedtaksperiodeId = UUID.randomUUID()
        dao.lagre(UUID.randomUUID(), "testKode", vedtaksperiodeId)
        assertEquals(1, dao.alleVarslerForVedtaksperiode(vedtaksperiodeId).size)
    }
}