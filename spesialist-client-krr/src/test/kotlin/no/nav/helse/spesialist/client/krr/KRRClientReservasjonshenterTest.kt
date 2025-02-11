package no.nav.helse.spesialist.client.krr

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.Reservasjonshenter.ReservasjonDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@WireMockTest
class KRRClientReservasjonshenterTest {
    @Test
    fun `tåler svar med ekstra felter`(wmRuntimeInfo: WireMockRuntimeInfo) {
        test(
            givenResponse = okJson("""{ "something-else": "?", "kanVarsles": false, "x": "YZ", "reservert": true }"""),
            expectedReservasjon = ReservasjonDto(kanVarsles = false, reservert = true),
            wmRuntimeInfo = wmRuntimeInfo
        )
    }

    @Test
    fun `mapper svar som forventet`(wmRuntimeInfo: WireMockRuntimeInfo) {
        test(
            givenResponse = okJson("""{ "kanVarsles": false, "reservert": true }"""),
            expectedReservasjon = ReservasjonDto(kanVarsles = false, reservert = true),
            wmRuntimeInfo = wmRuntimeInfo
        )
    }

    @Test
    fun `gir null om ett felt mangler`(wmRuntimeInfo: WireMockRuntimeInfo) {
        test(
            givenResponse = okJson("""{ "reservert": true }"""),
            expectedReservasjon = null,
            wmRuntimeInfo = wmRuntimeInfo
        )
    }

    @Test
    fun `gir null om ett felt har feil type`(wmRuntimeInfo: WireMockRuntimeInfo) {
        test(
            givenResponse = okJson("""{ "kanVarsles": "false", "reservert": true }"""),
            expectedReservasjon = null,
            wmRuntimeInfo = wmRuntimeInfo
        )
    }

    private fun test(
        givenResponse: ResponseDefinitionBuilder,
        expectedReservasjon: ReservasjonDto?,
        wmRuntimeInfo: WireMockRuntimeInfo
    ) {
        // Given:
        stubFor(get("/rest/v1/person").willReturn(givenResponse))
        val client = KRRClientReservasjonshenter(
            apiUrl = wmRuntimeInfo.httpBaseUrl,
            scope = "scoap",
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
