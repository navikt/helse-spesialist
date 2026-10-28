package no.nav.helse.spesialist.api.sse

import no.nav.helse.spesialist.api.IntegrationTestFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class SseOpenApiTest {
    private val integrationTestFixture = IntegrationTestFixture()

    @Test
    fun `SSE-ruta er dokumentert som GET med ApiServerSentEvent i OpenAPI-spesifikasjonen`() {
        val response = integrationTestFixture.get("/api/openapi.json")

        assertEquals(200, response.status)
        val spec = assertNotNull(response.bodyAsJsonNode)
        val sseRute = spec.path("paths").path("/api/personer/{personPseudoId}/sse")
        assertEquals(setOf("get"), sseRute.propertyNames().toSet())
        val skjemaer = spec.path("components").path("schemas")
        assertNotNull(skjemaer.get("ApiServerSentEvent"), "SSE-rute: $sseRute, skjemaer: ${skjemaer.propertyNames()}")
    }
}
