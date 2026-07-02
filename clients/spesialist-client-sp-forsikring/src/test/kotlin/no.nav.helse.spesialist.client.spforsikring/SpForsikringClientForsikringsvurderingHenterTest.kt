package no.nav.helse.spesialist.client.spforsikring

import no.nav.helse.spesialist.application.testfixtures.InMemoryAccessTokenProvider
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.Scenario
import no.nav.helse.spesialist.application.Forsikringsvurdering
import no.nav.helse.spesialist.domain.ForsikringsvurderingId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


class SpForsikringClientForsikringsvurderingHenterTest {
    @Suppress("JUnitMalformedDeclaration")
    @RegisterExtension
    private val wireMock: WireMockExtension =
        WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
            .build()

    val forsikringsvurderingId = ForsikringsvurderingId(UUID.randomUUID())

    @Test
    fun `mapper svar som forventet ved mottatt forsikring`() {
        // Given:
        val identitetsnummer = lagIdentitetsnummer()
        val client = setupStubAndClient(
            forsikringProxyResponse = okJson(
                """
                    {
                        "identitetsnummer": "${identitetsnummer.value}",
                        "harForsikring": true,
                        "dekning": { "grad": 100, "fraDag": 17 }
                    }
                """.trimIndent()
            ),
            forsikringsvurderingId = forsikringsvurderingId
        )

        // When:
        val actualForsikring = client.hent(forsikringsvurderingId = forsikringsvurderingId)

        // Then:
        assertEquals(
            expected = Forsikringsvurdering(
                identitetsnummer = identitetsnummer,
                harForsikring = true,
                dekning = Forsikringsvurdering.Dekning(grad = 100, fraDag = 17)
            ),
            actual = actualForsikring
        )
    }

    @Test
    fun `mapper svar som forventet ved ingen forsikring`() {
        // Given:
        val identitetsnummer = lagIdentitetsnummer()
        val client = setupStubAndClient(
            forsikringProxyResponse = okJson(
                """
                    {
                        "identitetsnummer": "${identitetsnummer.value}",
                        "harForsikring": false,
                        "dekning": null
                    }
                """.trimIndent()
            ),
            forsikringsvurderingId = forsikringsvurderingId
        )

        // When:
        val actualForsikring = client.hent(forsikringsvurderingId = forsikringsvurderingId)

        // Then:
        assertEquals(
            expected = Forsikringsvurdering(
                identitetsnummer = identitetsnummer,
                harForsikring = false,
                dekning = null
            ),
            actual = actualForsikring
        )
    }

    @Test
    fun `feiler om sp-forsikring gir tilbake HTTP 500`() {
        testMedForventningOmFeiletKall(
            stubResponse = WireMock.serverError().withBody("Her står det en feilmelding som ikke engang er JSON"),
            expectedException = RuntimeException("Feil fra forsikringstjeneste: 500"),
            forsikringsvurderingId = forsikringsvurderingId
        )
    }

    @Test
    fun `gir null om sp-forsikring gir tilbake HTTP 404`() {
        // Given:
        val client = setupStubAndClient(
            forsikringProxyResponse = WireMock.notFound().withBody("""{ "feil": "Finnes ikke" }"""),
            forsikringsvurderingId = forsikringsvurderingId
        )

        // When:
        val forsikringsvurdering = client.hent(forsikringsvurderingId = forsikringsvurderingId)

        assertNull(forsikringsvurdering)
    }

    @Test
    fun `får forsikring etter retry ved feil første kall`() {
        // Given:
        val identitetsnummer = lagIdentitetsnummer()
        // Given:
        wireMock.stubFor(
            get("/forsikringsvurderinger/${forsikringsvurderingId.value}").inScenario("scenario")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(WireMock.serverError().withBody("Her står det en feilmelding som ikke engang er JSON"))
                .willSetStateTo("har feilet én gang")
        )
        wireMock.stubFor(
            get("/forsikringsvurderinger/${forsikringsvurderingId.value}").inScenario("scenario")
                .whenScenarioStateIs("har feilet én gang")
                .willReturn(
                    okJson(
                        """
                            {
                                "identitetsnummer": "${identitetsnummer.value}",
                                "harForsikring": true,
                                "dekning": { "grad": 100, "fraDag": 17 }
                            }
                        """.trimIndent()
                    )
                )
        )
        val client = SpForsikringClientForsikringsvurderingHenter(
            configuration = ClientSpForsikringModule.Configuration(
                apiUrl = wireMock.runtimeInfo.httpBaseUrl,
                scope = "scoap"
            ),
            accessTokenProvider = InMemoryAccessTokenProvider("gief axess plz")
        )

        // When:
        val actualForsikring = client.hent(forsikringsvurderingId)

        // Then:
        wireMock.verify(2, getRequestedFor(urlEqualTo("/forsikringsvurderinger/${forsikringsvurderingId.value}")))
        assertEquals(
            expected = Forsikringsvurdering(
                identitetsnummer = identitetsnummer,
                harForsikring = true,
                dekning = Forsikringsvurdering.Dekning(grad = 100, fraDag = 17)
            ),
            actual = actualForsikring
        )
    }

    private fun testMedForventningOmFeiletKall(
        stubResponse: ResponseDefinitionBuilder?,
        forsikringsvurderingId: ForsikringsvurderingId,
        expectedException: Exception
    ) {
        val client = setupStubAndClient(stubResponse, forsikringsvurderingId)
        val actualException = runCatching {
            client.hent(forsikringsvurderingId)
        }.exceptionOrNull()

        assertNotNull(actualException)
        assertEquals(expectedException::class, actualException::class)
        assertEquals(expectedException.message, actualException.message)
    }

    private fun setupStubAndClient(
        forsikringProxyResponse: ResponseDefinitionBuilder?,
        forsikringsvurderingId: ForsikringsvurderingId
    ): SpForsikringClientForsikringsvurderingHenter {
        wireMock.stubFor(
            get("/forsikringsvurderinger/${forsikringsvurderingId.value}").willReturn(
                forsikringProxyResponse
            )
        )

        return SpForsikringClientForsikringsvurderingHenter(
            configuration = ClientSpForsikringModule.Configuration(
                apiUrl = wireMock.runtimeInfo.httpBaseUrl,
                scope = "scoap"
            ),
            accessTokenProvider = InMemoryAccessTokenProvider("gief axess plz")
        )
    }
}
