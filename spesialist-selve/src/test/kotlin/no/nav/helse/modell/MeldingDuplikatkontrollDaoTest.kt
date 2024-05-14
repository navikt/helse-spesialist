package no.nav.helse.modell

import DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class MeldingDuplikatkontrollDaoTest : DatabaseIntegrationTest() {
    @Test
    fun `sjekk om lagret melding oppfattes som behandlet`() {
        val meldingId = UUID.randomUUID()
        assertFalse(meldingDuplikatkontrollDao.erBehandlet(meldingId))
        meldingDuplikatkontrollDao.lagre(meldingId, "GODKJENNING")
        assertTrue(meldingDuplikatkontrollDao.erBehandlet(meldingId))
    }
}