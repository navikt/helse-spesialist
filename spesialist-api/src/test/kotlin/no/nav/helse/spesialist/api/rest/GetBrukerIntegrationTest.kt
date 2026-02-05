package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import kotlin.test.Test
import kotlin.test.assertEquals

class GetBrukerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()

    @Test
    fun `returnerer brukerens tilganger når man har alle tilganger`() {
        // When:
        val response = integrationTestFixture.get("api/bruker", tilganger = Tilgang.entries.toSet(), brukerroller = Brukerrolle.entries.toSet())
        assertEquals(HttpStatusCode.OK.value, response.status)

        // Then:
        val bruker = response.body<ApiBruker>()
        assertEquals(setOf(ApiTilgang.LES, ApiTilgang.SKRIV),bruker.tilganger )
        assertEquals(ApiBrukerrolle.entries.toSet(),bruker.brukerroller )
    }

    @Test
    fun `uten tilganger har man ikke tilgang`() {
        // When
        val response = integrationTestFixture.get("api/bruker", tilganger = emptySet())
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
    }
}
