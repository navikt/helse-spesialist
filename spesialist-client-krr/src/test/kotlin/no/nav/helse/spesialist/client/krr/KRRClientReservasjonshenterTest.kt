package no.nav.helse.spesialist.client.krr

import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.Reservasjonshenter.ReservasjonDto
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension


class KRRClientReservasjonshenterTest {
    @Suppress("JUnitMalformedDeclaration")
    @RegisterExtension
    private val wireMock: WireMockExtension =
        WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
            .build()

    @Test
    fun `tåler svar med ekstra felter`() {
        test(
            responseContent = """{ "something-else": "?", "kanVarsles": false, "x": "YZ", "reservert": true }""",
            expectedReservasjon = ReservasjonDto(kanVarsles = false, reservert = true),
        )
    }

    @Test
    fun `mapper svar som forventet`() {
        test(
            responseContent = """{ "kanVarsles": false, "reservert": true }""",
            expectedReservasjon = ReservasjonDto(kanVarsles = false, reservert = true),
        )
    }

    @Test
    fun `gir null om ett felt mangler`() {
        test(
            responseContent = """{ "reservert": true }""",
            expectedReservasjon = null,
        )
    }

    @Test
    fun `gir null om ett felt har feil type`() {
        test(
            responseContent = """{ "kanVarsles": "false", "reservert": true }""",
            expectedReservasjon = null,
        )
    }

    private fun test(
        @Language("json") responseContent: String,
        expectedReservasjon: ReservasjonDto?,
        personident: String = "12345678910",
    ) {
        // Given:
        wireMock.stubFor(post("/rest/v1/personer").willReturn(okResponse(personident, responseContent)))
        val client = KRRClientReservasjonshenter(
            ClientKrrModule.Configuration(apiUrl = wireMock.runtimeInfo.httpBaseUrl, scope = "scoap"),
            accessTokenGenerator = object : AccessTokenGenerator {
                override suspend fun hentAccessToken(scope: String) = "gief axess plz"
            }
        )

        // When:
        val actualReservasjon: ReservasjonDto? = runBlocking {
            client.hentForPerson(personident)
        }

        // Then:
        assertEquals(expectedReservasjon, actualReservasjon)
    }

    private fun okResponse(personident: String, contents: String) = okJson(""" { "personer": { "$personident": $contents }, "feil": {} } """)
}
