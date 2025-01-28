package no.nav.helse.db

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.DatabaseIntegrationTest
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType.NY_SAKSBEHANDLEROPPGAVE
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType.UTBETALING_ANNULLERING_OK
import no.nav.helse.spesialist.api.abonnement.UtbetalingPayload
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelsetype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class PgOpptegnelseRepositoryTest : DatabaseIntegrationTest() {
    private val opptegnelseRepository = DBSessionContext(session).opptegnelseRepository

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
        abonnementDao.opprettAbonnement(SAKSBEHANDLER_OID, AKTØR)
        opptegnelseRepository.opprettOpptegnelse(
            FNR,
            UTBETALING_PAYLOAD,
            UTBETALING_ANNULLERING_OK
        )

        opptegnelseRepository.opprettOpptegnelse(FNR, GODKJENNINGSBEHOV_PAYLOAD, NY_SAKSBEHANDLEROPPGAVE)

        val alle = opptegnelseRepository.finnOpptegnelser(SAKSBEHANDLER_OID)
        assertEquals(2, alle.size)

        alle.first { it.type == Opptegnelsetype.UTBETALING_ANNULLERING_OK }.also { opptegnelse ->
            assertEquals(AKTØR, opptegnelse.aktorId)
            assertJson(UTBETALING_PAYLOAD.toJson(), opptegnelse.payload)
        }

        alle.first { it.type == Opptegnelsetype.NY_SAKSBEHANDLEROPPGAVE }.also { opptegnelse ->
            assertEquals(AKTØR, opptegnelse.aktorId)
            assertJson(GODKJENNINGSBEHOV_PAYLOAD.toJson(), opptegnelse.payload)
        }
    }

    @Test
    fun `Skal ikke få opptegnelser fra før abonneringstidspunkt`() {
        opprettPerson()
        opprettSaksbehandler()
        opptegnelseRepository.opprettOpptegnelse(
            FNR,
            UTBETALING_PAYLOAD,
            UTBETALING_ANNULLERING_OK
        )
        abonnementDao.opprettAbonnement(SAKSBEHANDLER_OID, AKTØR)

        val alle = opptegnelseRepository.finnOpptegnelser(SAKSBEHANDLER_OID)
        assertEquals(0, alle.size)
    }
}
