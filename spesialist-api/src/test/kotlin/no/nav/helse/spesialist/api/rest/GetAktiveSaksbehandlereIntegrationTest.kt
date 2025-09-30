package no.nav.helse.spesialist.api.rest

import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertNotNull

class GetAktiveSaksbehandlereIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val saksbehandlerRepository = integrationTestFixture.sessionFactory.sessionContext.saksbehandlerRepository

    @Test
    fun `henter alle aktive saksbehandlere siste tre mnder`() {
        // Given:
        val saksbehandler1 = Saksbehandler(
            id = SaksbehandlerOid(UUID.randomUUID()),
            navn = "Navn Navnesen",
            epost = "navn@navnesen.no",
            ident = "L112233"
        )
        val saksbehandler2 = Saksbehandler(
            id = SaksbehandlerOid(UUID.randomUUID()),
            navn = "Test Testesen",
            epost = "test@testesen.no",
            ident = "L445566"
        )
        saksbehandlerRepository.lagre(saksbehandler1)
        saksbehandlerRepository.lagre(saksbehandler2)

        // When:
        val response = integrationTestFixture.get(
            url = "/api/aktive-saksbehandlere",
            saksbehandler = saksbehandler1
        )

        // Then:
        assertEquals(200, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        assertEquals(2, body.size())
    }
}
