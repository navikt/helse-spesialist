package no.nav.helse.modell

import DatabaseIntegrationTest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.abonnement.GodkjenningsbehovPayload
import no.nav.helse.abonnement.GodkjenningsbehovPayload.Companion.lagre
import no.nav.helse.abonnement.OpptegnelseType.NY_SAKSBEHANDLEROPPGAVE
import no.nav.helse.abonnement.OpptegnelseType.UTBETALING_ANNULLERING_OK
import no.nav.helse.abonnement.UtbetalingPayload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class OpptegnelseDaoTest : DatabaseIntegrationTest() {

    private companion object {
        private val UTBETALING_PAYLOAD = UtbetalingPayload(UUID.randomUUID())
        private val GODKJENNINGSBEHOV_PAYLOAD = GodkjenningsbehovPayload(UUID.randomUUID())
        private val objectMapper = jacksonObjectMapper()
        private fun assertJson(expected: String, actual: String) {
            assertEquals("${objectMapper.readTree(expected)}", "${objectMapper.readTree(actual)}")
        }
    }

    @Test
    fun `Kan opprette abonnement og få opptegnelser`() {
        opprettPerson()
        opprettSaksbehandler()
        abonnementDao.opprettAbonnement(SAKSBEHANDLER_OID, AKTØR.toLong())
        opptegnelseDao.opprettOpptegnelse(
            FNR,
            UTBETALING_PAYLOAD,
            UTBETALING_ANNULLERING_OK
        )

        GODKJENNINGSBEHOV_PAYLOAD.lagre(opptegnelseDao, FNR)

        val alle = opptegnelseDao.finnOpptegnelser(SAKSBEHANDLER_OID)
        assertEquals(2, alle.size)

        alle.first { it.type == UTBETALING_ANNULLERING_OK }.also { opptegnelse ->
            assertEquals(AKTØR.toLong(), opptegnelse.aktørId)
            assertJson(UTBETALING_PAYLOAD.toJson(), opptegnelse.payload)
        }

        alle.first { it.type == NY_SAKSBEHANDLEROPPGAVE }.also { opptegnelse ->
            assertEquals(AKTØR.toLong(), opptegnelse.aktørId)
            assertJson(GODKJENNINGSBEHOV_PAYLOAD.toJson(), opptegnelse.payload)
        }
    }

    @Test
    fun `Skal ikke få opptegnelser fra før abonneringstidspunkt`() {
        opprettPerson()
        opprettSaksbehandler()
        opptegnelseDao.opprettOpptegnelse(
            FNR,
            UTBETALING_PAYLOAD,
            UTBETALING_ANNULLERING_OK
        )
        abonnementDao.opprettAbonnement(SAKSBEHANDLER_OID, AKTØR.toLong())

        val alle = opptegnelseDao.finnOpptegnelser(SAKSBEHANDLER_OID)
        assertEquals(0, alle.size)
    }
}
