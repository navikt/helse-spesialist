package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import kotlin.test.Test
import kotlin.test.assertEquals

class GetBrukerrollerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()

    @Test
    fun `returnerer brukerens roller`() {
        // When: Kaller GET /api/brukerroller
        val response = integrationTestFixture.get("api/brukerroller", brukerroller = Brukerrolle.entries.toSet())
        assertEquals(HttpStatusCode.OK.value, response.status)

        // Then: Responsen inneholder de forventede rollene
        val roller = response.body<Set<String>>()
        assertEquals(Brukerrolle.entries.map { it.name }.toSet(), roller)
    }

    @Test
    fun `returnerer tom liste når bruker ikke har roller`() {
        // When: Kaller GET /api/brukerroller
        val response = integrationTestFixture.get("api/brukerroller", brukerroller = emptySet())
        assertEquals(HttpStatusCode.OK.value, response.status)

        // Then: Responsen inneholder ingen roller
        val roller = response.body<Set<String>>()
        assertEquals(emptySet(), roller)
    }
}
