package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import kotlin.test.Test
import kotlin.test.assertEquals

class GetBrukerrollerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()

    @Test
    fun `returnerer brukerens tilganger når man har alle tilganger`() {
        // When: Kaller GET /api/brukerroller
        val response = integrationTestFixture.get("api/brukerroller", tilganger = Tilgang.entries.toSet())
        assertEquals(HttpStatusCode.OK.value, response.status)

        // Then: Responsen inneholder de forventede rollene
        val roller = response.body<Set<String>>()
        assertEquals(setOf("LESETILGANG", "SAKSBEHANDLER"), roller)
    }

    @Test
    fun `uten tilganger har man ikke tilgang`() {
        // When: Kaller GET /api/brukerroller
        val response = integrationTestFixture.get("api/brukerroller", tilganger = emptySet())
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
    }
}
