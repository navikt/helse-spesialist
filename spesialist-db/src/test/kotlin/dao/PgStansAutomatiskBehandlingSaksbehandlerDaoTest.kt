package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PgStansAutomatiskBehandlingSaksbehandlerDaoTest : AbstractDBIntegrationTest() {
    @Test
    fun `kan stanse automatisk behandling`() {
        val fødselsnummer = opprettPerson().id.value
        stansAutomatiskBehandlingSaksbehandlerDao.lagreStans(fødselsnummer)
        assertTrue(stansAutomatiskBehandlingSaksbehandlerDao.erStanset(fødselsnummer))
    }

    @Test
    fun `kan oppheve stans av automatisk behandling`() {
        val fødselsnummer = opprettPerson().id.value
        stansAutomatiskBehandlingSaksbehandlerDao.lagreStans(fødselsnummer)
        assertTrue(stansAutomatiskBehandlingSaksbehandlerDao.erStanset(fødselsnummer))
        stansAutomatiskBehandlingSaksbehandlerDao.opphevStans(fødselsnummer)
        assertFalse(stansAutomatiskBehandlingSaksbehandlerDao.erStanset(fødselsnummer))
    }
}
