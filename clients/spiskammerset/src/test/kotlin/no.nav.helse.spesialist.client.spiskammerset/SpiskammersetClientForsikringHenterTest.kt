package no.nav.helse.spesialist.client.spiskammerset

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.noContent
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import no.nav.helse.spesialist.domain.Forsikring
import no.nav.helse.spesialist.domain.ResultatAvForsikring
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull


class SpiskammersetClientForsikringHenterTest {
    @Suppress("JUnitMalformedDeclaration")
    @RegisterExtension
    private val wireMock: WireMockExtension =
        WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
            .build()

    val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())

    @Test
    fun `mapper svar som forventet ved mottatt forsikring`() {
        testMedForventningOmMottattForsikring(
            stubResponse = okJson(
                """
                    {
                      "dag1Eller17": "17",
                      "dekningsgrad": "100"
                    }
                """.trimIndent()
            ),
            spleisBehandlingId = spleisBehandlingId,
            expectedResultatAvForsikring = ResultatAvForsikring.MottattForsikring(Forsikring(17, 100))
        )
    }

    @Test
    fun `mapper svar som forventet ved ingen forsikring`() {
        testMedForventningOmIngenForsikring(
            stubResponse = noContent(),
            spleisBehandlingId = spleisBehandlingId,
        )
    }

    @Test
    fun `feiler om spiskammerset gir tilbake HTTP 500`() {
        testMedForventningOmFeiletKall(
            stubResponse = WireMock.serverError().withBody("Her står det en feilmelding som ikke engang er JSON"),
            expectedException = RuntimeException("Feil fra forsikringstjeneste: 500"),
            spleisBehandlingId = spleisBehandlingId
        )
    }

    @Test
    fun `feiler om spiskammerset gir tilbake HTTP 404`() {
        testMedForventningOmFeiletKall(
            stubResponse = WireMock.notFound().withBody("Her står det en feilmelding som ikke engang er JSON"),
            expectedException = RuntimeException("Feil fra forsikringstjeneste: 404"),
            spleisBehandlingId = spleisBehandlingId
        )
    }

    @Test
    fun `får forsikring etter retry ved feil første kall`() {
        // Given:
        val scenario = "Feiler først, så ok"
        wireMock.stubFor(
            get("/behandling/${spleisBehandlingId.value}/forsikring").inScenario(scenario).willReturn(
                WireMock.serverError().withBody("Her står det en feilmelding som ikke engang er JSON")
            ).willSetStateTo("har feilet")
        )
        wireMock.stubFor(
            get("/behandling/${spleisBehandlingId.value}/forsikring").inScenario(scenario)
                .whenScenarioStateIs("har feilet")
                .willReturn(
                    okJson(
                        """
                    {
                      "dag1Eller17": "17",
                      "dekningsgrad": "100"
                    }
                """.trimIndent()
                    )
                )
        )
        val client = SpiskammersetClientForsikringHenter(
            configuration = ClientSpiskammersetModule.Configuration(
                apiUrl = wireMock.runtimeInfo.httpBaseUrl,
                scope = "scoap"
            ),
            accessTokenGenerator = { "gief axess plz" }
        )

        // When:
        val result = client.hentForsikringsinformasjon(spleisBehandlingId)

        // Then:
        wireMock.verify(2, getRequestedFor(urlEqualTo("/behandling/${spleisBehandlingId.value}/forsikring")))
        assertIs<ResultatAvForsikring.MottattForsikring>(result)
        assertEquals(17, result.forsikring.gjelderFraDag)
        assertEquals(100, result.forsikring.dekningsgrad)
    }


    private fun testMedForventningOmMottattForsikring(
        stubResponse: ResponseDefinitionBuilder?,
        spleisBehandlingId: SpleisBehandlingId,
        expectedResultatAvForsikring: ResultatAvForsikring.MottattForsikring
    ) {
        // Given:
        val client = setupStubAndClient(stubResponse, spleisBehandlingId)

        // When:
        val actualResultatAvForsikring = client.hentForsikringsinformasjon(spleisBehandlingId)

        // Then:
        val mottatt = assertIs<ResultatAvForsikring.MottattForsikring>(actualResultatAvForsikring)
        assertEquals(expectedResultatAvForsikring.forsikring.gjelderFraDag, mottatt.forsikring.gjelderFraDag)
        assertEquals(expectedResultatAvForsikring.forsikring.dekningsgrad, mottatt.forsikring.dekningsgrad)
    }

    private fun testMedForventningOmIngenForsikring(
        stubResponse: ResponseDefinitionBuilder?,
        spleisBehandlingId: SpleisBehandlingId,
    ) {
        val client = setupStubAndClient(stubResponse, spleisBehandlingId)

        val actualForsikring = client.hentForsikringsinformasjon(spleisBehandlingId)

        assertEquals(ResultatAvForsikring.IngenForsikring, actualForsikring)
    }

    private fun testMedForventningOmFeiletKall(
        stubResponse: ResponseDefinitionBuilder?,
        spleisBehandlingId: SpleisBehandlingId,
        expectedException: Exception
    ) {
        val client = setupStubAndClient(stubResponse, spleisBehandlingId)
        val actualException = runCatching {
            client.hentForsikringsinformasjon(spleisBehandlingId)
        }.exceptionOrNull()

        assertNotNull(actualException)
        assertEquals(expectedException::class, actualException::class)
        assertEquals(expectedException.message, actualException.message)
    }

    private fun setupStubAndClient(
        forsikringProxyResponse: ResponseDefinitionBuilder?,
        spleisBehandlingId: SpleisBehandlingId
    ): SpiskammersetClientForsikringHenter {
        wireMock.stubFor(get("/behandling/${spleisBehandlingId.value}/forsikring").willReturn(forsikringProxyResponse))

        return SpiskammersetClientForsikringHenter(
            configuration = ClientSpiskammersetModule.Configuration(
                apiUrl = wireMock.runtimeInfo.httpBaseUrl,
                scope = "scoap"
            ),
            accessTokenGenerator = { "gief axess plz" }
        )
    }
}
