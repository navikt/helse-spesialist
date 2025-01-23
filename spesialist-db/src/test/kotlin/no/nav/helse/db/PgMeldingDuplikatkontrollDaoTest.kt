package no.nav.helse.db

import no.nav.helse.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class PgMeldingDuplikatkontrollDaoTest : DatabaseIntegrationTest() {
    @Test
    fun `sjekk om lagret melding oppfattes som behandlet`() {
        val meldingId = UUID.randomUUID()
        assertFalse(meldingDuplikatkontrollDao.erBehandlet(meldingId))
        meldingDuplikatkontrollDao.lagre(meldingId, "GODKJENNING")
        assertTrue(meldingDuplikatkontrollDao.erBehandlet(meldingId))
    }
}
