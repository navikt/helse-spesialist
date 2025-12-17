package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.OpptegnelseDao.Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE
import no.nav.helse.db.OpptegnelseDao.Opptegnelse.Type.UTBETALING_ANNULLERING_OK
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload
import no.nav.helse.spesialist.api.abonnement.UtbetalingPayload
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.db.DBSessionContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class PgOpptegnelseDaoTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()
    private val opptegnelseRepository = DBSessionContext(session).opptegnelseDao

    private companion object {
        private val UTBETALING_PAYLOAD = UtbetalingPayload(UUID.randomUUID()).toJson()
        private val GODKJENNINGSBEHOV_PAYLOAD = GodkjenningsbehovPayload(UUID.randomUUID()).toJson()
    }

    @Test
    fun `Kan opprette abonnement og få opptegnelser`() {
        opprettSaksbehandler()
        abonnementDao.opprettAbonnement(SAKSBEHANDLER_OID, person.aktørId)
        opptegnelseRepository.opprettOpptegnelse(
            person.id.value,
            UTBETALING_PAYLOAD,
            UTBETALING_ANNULLERING_OK,
        )

        opptegnelseRepository.opprettOpptegnelse(person.id.value, GODKJENNINGSBEHOV_PAYLOAD, NY_SAKSBEHANDLEROPPGAVE)

        val alle = opptegnelseRepository.finnOpptegnelser(SAKSBEHANDLER_OID)
        assertEquals(2, alle.size)

        alle.first { it.type == UTBETALING_ANNULLERING_OK }.also { opptegnelse ->
            assertEquals(person.aktørId, opptegnelse.aktorId)
            assertJsonEquals(UTBETALING_PAYLOAD, opptegnelse.payload)
        }

        alle.first { it.type == NY_SAKSBEHANDLEROPPGAVE }.also { opptegnelse ->
            assertEquals(person.aktørId, opptegnelse.aktorId)
            assertJsonEquals(GODKJENNINGSBEHOV_PAYLOAD, opptegnelse.payload)
        }
    }

    @Test
    fun `Skal ikke få opptegnelser fra før abonneringstidspunkt`() {
        opprettPerson()
        opprettSaksbehandler()
        opptegnelseRepository.opprettOpptegnelse(
            person.id.value,
            UTBETALING_PAYLOAD,
            UTBETALING_ANNULLERING_OK,
        )
        abonnementDao.opprettAbonnement(SAKSBEHANDLER_OID, person.aktørId)

        val alle = opptegnelseRepository.finnOpptegnelser(SAKSBEHANDLER_OID)
        assertEquals(0, alle.size)
    }
}
