package no.nav.helse.spesialist.api.rest

import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class GetAktiveSaksbehandlereIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val saksbehandlerRepository = integrationTestFixture.sessionFactory.sessionContext.saksbehandlerRepository

    @Test
    fun `henter alle aktive saksbehandlere siste tre mnder`() {
        // Given:
        val saksbehandler1 = lagSaksbehandler()
        val saksbehandler2 = lagSaksbehandler()
        saksbehandlerRepository.lagre(saksbehandler1)
        saksbehandlerRepository.lagre(saksbehandler2)

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/aktive-saksbehandlere",
                saksbehandler = saksbehandler1,
                brukerroller = setOf(Brukerrolle.SAKSBEHANDLER),
            )

        // Then:
        assertEquals(200, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        assertEquals(2, body.size())
    }
}
