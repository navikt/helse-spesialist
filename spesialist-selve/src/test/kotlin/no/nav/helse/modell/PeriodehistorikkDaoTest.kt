package no.nav.helse.modell

import DatabaseIntegrationTest
import java.util.UUID
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PeriodehistorikkDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `lagre og finn periodehistorikk`() {
        val periodeId = UUID.randomUUID()
        opprettSaksbehandler()

        val insertCount = periodehistorikkDao.lagre(
            PeriodehistorikkType.TOTRINNSVURDERING_TIL_GODKJENNING,
            SAKSBEHANDLER_OID,
            periodeId
        )
        val result = periodehistorikkDao.finn(periodeId)

        assertEquals(insertCount, result.size)
    }

}
