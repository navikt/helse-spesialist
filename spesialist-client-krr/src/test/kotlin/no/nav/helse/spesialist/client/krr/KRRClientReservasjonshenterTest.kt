package no.nav.helse.spesialist.client.krr

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.Reservasjonshenter.ReservasjonDto
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
            givenResponse = okJson("""{ "something-else": "?", "kanVarsles": false, "x": "YZ", "reservert": true }"""),
            expectedReservasjon = ReservasjonDto(kanVarsles = false, reservert = true)
        )
    }

    @Test
    fun `mapper svar som forventet`() {
        test(
            givenResponse = okJson("""{ "kanVarsles": false, "reservert": true }"""),
            expectedReservasjon = ReservasjonDto(kanVarsles = false, reservert = true)
        )
    }

    @Test
    fun `gir null om ett felt mangler`() {
        test(
            givenResponse = okJson("""{ "reservert": true }"""),
            expectedReservasjon = null
        )
    }

    @Test
    fun `gir null om ett felt har feil type`() {
        test(
            givenResponse = okJson("""{ "kanVarsles": "false", "reservert": true }"""),
            expectedReservasjon = null
        )
    }

    private fun test(
        givenResponse: ResponseDefinitionBuilder,
        expectedReservasjon: ReservasjonDto?
    ) {
        // Given:
        wireMock.stubFor(get("/rest/v1/person").willReturn(givenResponse))
        val client = KRRClientReservasjonshenter(
            ClientKrrModule.Configuration.Client(apiUrl = wireMock.runtimeInfo.httpBaseUrl, scope = "scoap"),
            accessTokenGenerator = object : AccessTokenGenerator {
                override suspend fun hentAccessToken(scope: String) = "gief axess plz"
            }
        )

        // When:
        val actualReservasjon: ReservasjonDto? = runBlocking {
            client.hentForPerson("12345678910")
        }

        // Then:
        assertEquals(expectedReservasjon, actualReservasjon)
    }
}
