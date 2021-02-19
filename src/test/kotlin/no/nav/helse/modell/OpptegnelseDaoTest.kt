package no.nav.helse.modell

import DatabaseIntegrationTest
import no.nav.helse.modell.abonnement.OpptegnelseType.UTBETALING_ANNULLERING_OK
import no.nav.helse.modell.abonnement.UtbetalingPayload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class OpptegnelseDaoTest : DatabaseIntegrationTest() {

    companion object {
        private val PAYLOAD = UtbetalingPayload(UUID.randomUUID())
    }

    @Test
    fun `Får opprettet opptegnelse`() {
        opprettPerson(fødselsnummer = FNR)
        opptegnelseDao.opprettOpptegnelse(
            FNR,
            PAYLOAD,
            UTBETALING_ANNULLERING_OK
        )

        val alle = opptegnelseDao.alleOpptegnelser()
        assertEquals(1, alle.size)
    }

    @Test
    fun `Kan abonnere`() {
        opprettPerson(aktørId = AKTØR)
        opprettSaksbehandler(saksbehandlerOID = SAKSBEHANDLER_OID)
        opptegnelseDao.opprettAbonnement(SAKSBEHANDLER_OID, AKTØR.toLong())

        val abonnementForSaksbehandler = opptegnelseDao.finnAbonnement(SAKSBEHANDLER_OID)
        assertEquals(1, abonnementForSaksbehandler.size)
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
        opptegnelseDao.opprettAbonnement(SAKSBEHANDLER_OID, AKTØR.toLong())

        val alle = opptegnelseDao.finnOpptegnelser(SAKSBEHANDLER_OID)
        assertEquals(0, alle.size)
    }
}
