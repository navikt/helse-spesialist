package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.Test
import kotlin.test.assertEquals

class GetBrukerrollerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()

    @Test
    fun `returnerer brukerens roller når man har alle roller`() {
        // When: Kaller GET /api/brukerroller
        val response = integrationTestFixture.get("api/brukerroller", brukerroller = Brukerrolle.entries.toSet())
        assertEquals(HttpStatusCode.OK.value, response.status)

        // Then: Responsen inneholder de forventede rollene
        val roller = response.body<Set<String>>()
        assertEquals(Brukerrolle.entries.map { it.name }.toSet(), roller)
    }

    @ParameterizedTest
    @EnumSource(Brukerrolle::class)
    fun `alle roller har tilgang til endepunktet`(rolle: Brukerrolle) {
        // When: Kaller GET /api/brukerroller
        val response = integrationTestFixture.get("api/brukerroller", brukerroller = setOf(rolle))
        assertEquals(HttpStatusCode.OK.value, response.status)

        // Then: Responsen inneholder de forventede rollene
        val roller = response.body<Set<String>>()
        assertEquals(setOf(rolle.name), roller)
    }

    @Test
    fun `uten roller har man ikke tilgang`() {
        // When: Kaller GET /api/brukerroller
        val response = integrationTestFixture.get("api/brukerroller", brukerroller = emptySet())
        assertEquals(HttpStatusCode.Forbidden.value, response.status)

    }
}
