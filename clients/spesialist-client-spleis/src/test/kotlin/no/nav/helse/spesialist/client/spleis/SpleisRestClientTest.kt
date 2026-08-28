package no.nav.helse.spesialist.client.spleis

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.notFound
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import no.nav.helse.spesialist.application.testfixtures.InMemoryAccessTokenProvider
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SpleisRestClientTest {
    @Suppress("JUnitMalformedDeclaration")
    @RegisterExtension
    private val wireMock: WireMockExtension =
        WireMockExtension
            .newInstance()
            .options(wireMockConfig().dynamicPort())
            .build()

    @Test
    fun `returnerer person ved vellykket svar`() {
        setupStub(
            okJson(
                """
                {
                  "aktorId": "1234567890123",
                  "fodselsnummer": "11111111111",
                  "arbeidsgivere": [],
                  "dodsdato": null,
                  "versjon": 1,
                  "vilkarsgrunnlag": []
                }
                """.trimIndent(),
            ),
        )

        val result = lagKlient().hentPerson("11111111111")

        assertNotNull(result)
        assertEquals("1234567890123", result.aktorId)
        assertEquals("11111111111", result.fodselsnummer)
        assertEquals(1, result.versjon)
    }

    @Test
    fun `returnerer null ved 404`() {
        setupStub(notFound())

        val result = lagKlient().hentPerson("11111111111")

        assertNull(result)
    }

    @Test
    fun `feiler ved HTTP 500`() {
        setupStub(serverError().withBody("Intern feil"))

        val exception =
            runCatching {
                lagKlient().hentPerson("11111111111")
            }.exceptionOrNull()

        assertNotNull(exception)
        assertIs<IllegalStateException>(exception)
    }

    private fun setupStub(response: ResponseDefinitionBuilder) {
        wireMock.stubFor(post(urlEqualTo("/api/person")).willReturn(response))
    }

    private fun lagKlient() =
        SpleisRestClient(
            accessTokenProvider = InMemoryAccessTokenProvider("test-token"),
            spleisUrl = URI.create(wireMock.runtimeInfo.httpBaseUrl),
            spleisClientId = "local-app",
        )
}
