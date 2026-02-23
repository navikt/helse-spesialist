package no.nav.helse.spesialist.client.speed

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import io.mockk.mockk
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SpeedClientHistoriskeIdenterHenterTest {
    @Suppress("JUnitMalformedDeclaration")
    @RegisterExtension
    private val wireMock: WireMockExtension =
        WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build()

    @Test
    fun `returnerer fødselsnumre ved vellykket svar`() {
        setupStub(
            okJson(
                """{ "fødselsnumre": ["11111111111", "22222222222"], "kilde": "PDL" }""",
            ),
        )

        val result = lagKlient().hentHistoriskeIdenter("11111111111")

        assertEquals(listOf("11111111111", "22222222222"), result)
    }

    @Test
    fun `returnerer tom liste ved 404`() {
        setupStub(com.github.tomakehurst.wiremock.client.WireMock.notFound())

        val result = lagKlient().hentHistoriskeIdenter("99999999999")

        assertEquals(emptyList(), result)
    }

    @Test
    fun `feiler ved HTTP 500`() {
        setupStub(serverError().withBody("Intern feil"))

        val exception =
            runCatching {
                lagKlient().hentHistoriskeIdenter("11111111111")
            }.exceptionOrNull()

        assertNotNull(exception)
        assertIs<RuntimeException>(exception)
    }

    private fun setupStub(response: ResponseDefinitionBuilder) {
        wireMock.stubFor(post(urlEqualTo("/api/historiske_identer")).willReturn(response))
    }

    private fun lagKlient() =
        SpeedClientHistoriskeIdenterHenter(
            configuration =
                ClientSpeedModule.Configuration(
                    apiUrl = wireMock.runtimeInfo.httpBaseUrl,
                    scope = "scoap",
                ),
            accessTokenGenerator = { "test-token" },
            environmentToggles = mockk(relaxed = true),
        )
}
