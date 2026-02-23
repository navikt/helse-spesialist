package no.nav.helse.spesialist.client.speed

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import no.nav.helse.spesialist.domain.Personinfo
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SpeedClientPersoninfoHenterTest {
    @Suppress("JUnitMalformedDeclaration")
    @RegisterExtension
    private val wireMock: WireMockExtension =
        WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build()

    @Test
    fun `returnerer personinfo ved vellykket svar`() {
        setupStub(
            okJson(
                """{ "fødselsdato": "1990-01-15", "dødsdato": null, "fornavn": "Ola", "mellomnavn": null, "etternavn": "Nordmann", "adressebeskyttelse": "UGRADERT", "kjønn": "MANN", "kilde": "PDL" }""",
            ),
        )

        val result = lagKlient().hentPersoninfo("11111111111")

        assertNotNull(result)
        assertEquals("Ola", result.fornavn)
        assertEquals("Nordmann", result.etternavn)
        assertEquals(LocalDate.of(1990, 1, 15), result.fødselsdato)
        assertEquals(Personinfo.Kjønn.Mann, result.kjønn)
        assertEquals(Personinfo.Adressebeskyttelse.Ugradert, result.adressebeskyttelse)
    }

    @Test
    fun `returnerer null ved 404`() {
        setupStub(com.github.tomakehurst.wiremock.client.WireMock.notFound())

        val result = lagKlient().hentPersoninfo("99999999999")

        assertNull(result)
    }

    @Test
    fun `feiler ved HTTP 500`() {
        setupStub(serverError().withBody("Intern feil"))

        val exception =
            runCatching {
                lagKlient().hentPersoninfo("11111111111")
            }.exceptionOrNull()

        assertNotNull(exception)
        assertIs<RuntimeException>(exception)
    }

    private fun setupStub(response: ResponseDefinitionBuilder) {
        wireMock.stubFor(post(urlEqualTo("/api/person")).willReturn(response))
    }

    private fun lagKlient() =
        SpeedClientPersoninfoHenter(
            configuration =
                ClientSpeedModule.Configuration(
                    apiUrl = wireMock.runtimeInfo.httpBaseUrl,
                    scope = "scoap",
                ),
            accessTokenGenerator = { "test-token" },
        )
}
