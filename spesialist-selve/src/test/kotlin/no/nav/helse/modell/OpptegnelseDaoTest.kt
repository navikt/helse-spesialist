package no.nav.helse.modell

import DatabaseIntegrationTest
import no.nav.helse.abonnement.OpptegnelseType.UTBETALING_ANNULLERING_OK
import no.nav.helse.modell.opptegnelse.UtbetalingPayload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class OpptegnelseDaoTest : DatabaseIntegrationTest() {

    companion object {
        private val PAYLOAD = UtbetalingPayload(UUID.randomUUID())
    }

    @Test
    fun `Kan opprette abonnement og få opptegnelser`() {
        opprettPerson()
        opprettSaksbehandler()
        abonnementDao.opprettAbonnement(SAKSBEHANDLER_OID, AKTØR.toLong())
        opptegnelseDao.opprettOpptegnelse(
            FNR,
            PAYLOAD,
            UTBETALING_ANNULLERING_OK
        )

        val alle = opptegnelseApiDao.finnOpptegnelser(SAKSBEHANDLER_OID)
        assertEquals(1, alle.size)
    }

    @Test
    fun `Skal ikke få opptegnelser fra før abonneringstidspunkt`() {
        opprettPerson()
        opprettSaksbehandler()
        opptegnelseDao.opprettOpptegnelse(
            FNR,
            PAYLOAD,
            UTBETALING_ANNULLERING_OK
        )
        abonnementDao.opprettAbonnement(SAKSBEHANDLER_OID, AKTØR.toLong())

        val alle = opptegnelseApiDao.finnOpptegnelser(SAKSBEHANDLER_OID)
        assertEquals(0, alle.size)
    }
}
