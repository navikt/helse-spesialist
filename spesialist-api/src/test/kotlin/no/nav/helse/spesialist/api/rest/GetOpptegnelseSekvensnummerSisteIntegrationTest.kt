package no.nav.helse.spesialist.api.rest

import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class GetOpptegnelseSekvensnummerSisteIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val opptegnelseRepository = integrationTestFixture.sessionFactory.sessionContext.opptegnelseRepository

    @Test
    fun `henter siste sekvensnummer`() {
        // Given:
        val opptegnelse1 = Opptegnelse.ny(
            identitetsnummer = lagIdentitetsnummer(),
            type = Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE,
            payload = "{json}",
        )
        val opptegnelse2 = Opptegnelse.ny(
            identitetsnummer = lagIdentitetsnummer(),
            type = Opptegnelse.Type.FERDIGBEHANDLET_GODKJENNINGSBEHOV,
            payload = "{json}",
        )
        opptegnelseRepository.lagre(opptegnelse1)
        opptegnelseRepository.lagre(opptegnelse2)

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/opptegnelse-sekvensnummer/siste",
            )

        // Then:
        assertEquals(200, response.status)
        val body = response.bodyAsText
        assertNotNull(body)
        assertEquals("2", body)
    }
}
