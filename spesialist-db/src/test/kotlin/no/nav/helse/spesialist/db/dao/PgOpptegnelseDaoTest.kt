package no.nav.helse.spesialist.db.dao

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.db.DBSessionContext
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.OpptegnelseDao.Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE
import no.nav.helse.db.OpptegnelseDao.Opptegnelse.Type.UTBETALING_ANNULLERING_OK
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload
import no.nav.helse.spesialist.api.abonnement.UtbetalingPayload
import no.nav.helse.spesialist.db.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class PgOpptegnelseDaoTest : DatabaseIntegrationTest() {
    private val opptegnelseRepository = DBSessionContext(session) { _, _ -> false }.opptegnelseDao

    private companion object {
        private val UTBETALING_PAYLOAD = UtbetalingPayload(UUID.randomUUID()).toJson()
        private val GODKJENNINGSBEHOV_PAYLOAD = GodkjenningsbehovPayload(UUID.randomUUID()).toJson()
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

        alle.first { it.type == OpptegnelseDao.Opptegnelse.Type.UTBETALING_ANNULLERING_OK }.also { opptegnelse ->
            assertEquals(AKTØR, opptegnelse.aktorId)
            assertJson(UTBETALING_PAYLOAD, opptegnelse.payload)
        }

        alle.first { it.type == OpptegnelseDao.Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE }.also { opptegnelse ->
            assertEquals(AKTØR, opptegnelse.aktorId)
            assertJson(GODKJENNINGSBEHOV_PAYLOAD, opptegnelse.payload)
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
