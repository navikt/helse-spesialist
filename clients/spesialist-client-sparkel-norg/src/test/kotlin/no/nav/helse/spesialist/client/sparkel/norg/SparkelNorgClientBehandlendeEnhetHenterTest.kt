package no.nav.helse.spesialist.client.sparkel.norg

import com.fasterxml.jackson.core.type.TypeReference
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import no.nav.helse.spesialist.application.Cache
import no.nav.helse.spesialist.domain.Enhet
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SparkelNorgClientBehandlendeEnhetHenterTest {
    private val identitetsnummer = lagIdentitetsnummer()

    @Suppress("JUnitMalformedDeclaration")
    @RegisterExtension
    private val wireMock: WireMockExtension =
        WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
            .build()

    @Test
    fun `mapper svar som forventet`() {
        testMedForventningOmVellykketKall(
            stubResponse =
                okJson(
                    """
                    {
                      "enhetNr": "1337",
                      "navn": "Nav Gamle Oslo",
                      "type": "LOKAL"
                    }
                    """.trimIndent(),
                ),
            expectedEnhet =
                Enhet(
                    enhetNr = "1337",
                    navn = "Nav Gamle Oslo",
                    type = "LOKAL",
                ),
        )
    }

    @Test
    fun `tåler svar med ekstra felter`() {
        testMedForventningOmVellykketKall(
            stubResponse =
                okJson(
                    """
                    {
                      "enhetNr": "1337",
                      "navn": "Nav Gamle Oslo",
                      "type": "LOKAL",
                      "ekstraFelt": "ignoreres"
                    }
                    """.trimIndent(),
                ),
            expectedEnhet =
                Enhet(
                    enhetNr = "1337",
                    navn = "Nav Gamle Oslo",
                    type = "LOKAL",
                ),
        )
    }

    @Test
    fun `returnerer null ved HTTP 404`() {
        val client = setupStubAndClient(WireMock.notFound())
        val result = client.hentFor(identitetsnummer)
        assertNull(result)
    }

    @Test
    fun `feiler om feltet enhetNr mangler`() {
        testMedForventningOmFeiletKall(
            stubResponse =
                okJson(
                    """
                    {
                      "navn": "Nav Gamle Oslo",
                      "type": "LOKAL"
                    }
                    """.trimIndent(),
                ),
            expectedException = IllegalStateException("Fant ikke feltet enhetNr i responsen fra sparkel-norg"),
        )
    }

    @Test
    fun `feiler om feltet navn mangler`() {
        testMedForventningOmFeiletKall(
            stubResponse =
                okJson(
                    """
                    {
                      "enhetNr": "1337",
                      "type": "LOKAL"
                    }
                    """.trimIndent(),
                ),
            expectedException = IllegalStateException("Fant ikke feltet navn i responsen fra sparkel-norg"),
        )
    }

    @Test
    fun `feiler om feltet type mangler`() {
        testMedForventningOmFeiletKall(
            stubResponse =
                okJson(
                    """
                    {
                      "enhetNr": "1337",
                      "navn": "Nav Gamle Oslo"
                    }
                    """.trimIndent(),
                ),
            expectedException = IllegalStateException("Fant ikke feltet type i responsen fra sparkel-norg"),
        )
    }

    @Test
    fun `feiler om sparkel-norg gir tilbake HTTP 500`() {
        testMedForventningOmFeiletKall(
            stubResponse = WireMock.serverError().withBody("Her står det en feilmelding som ikke engang er JSON"),
            expectedException = IllegalStateException("Fikk HTTP 500 tilbake fra sparkel-norg"),
        )
    }

    private fun testMedForventningOmVellykketKall(
        stubResponse: ResponseDefinitionBuilder?,
        expectedEnhet: Enhet,
    ) {
        val client = setupStubAndClient(stubResponse)

        val actualEnhet = client.hentFor(identitetsnummer)

        assertEquals(expectedEnhet, actualEnhet)
    }

    private fun testMedForventningOmFeiletKall(
        stubResponse: ResponseDefinitionBuilder?,
        expectedException: Exception,
    ) {
        val client = setupStubAndClient(stubResponse)

        val actualException =
            runCatching {
                client.hentFor(identitetsnummer)
            }.exceptionOrNull()

        assertNotNull(actualException)
        assertEquals(expectedException::class, actualException::class)
        assertEquals(expectedException.message, actualException.message)
    }

    private fun setupStubAndClient(sparkelNorgResponse: ResponseDefinitionBuilder?): SparkelNorgClientBehandlendeEnhetHenter {
        wireMock.stubFor(post("/api/behandlende-enhet").willReturn(sparkelNorgResponse))

        return SparkelNorgClientBehandlendeEnhetHenter(
            configuration =
                ClientSparkelNorgModule.Configuration(
                    apiUrl = wireMock.runtimeInfo.httpBaseUrl,
                    scope = "scoap",
                ),
            accessTokenGenerator = { "gief axess plz" },
            cache =
                object : Cache {
                    override fun <T> hentGjennomCache(
                        namespace: String,
                        id: String,
                        type: TypeReference<T>,
                        timeToLive: java.time.Duration,
                        hentUtenomCache: () -> T,
                    ) = hentUtenomCache()
                },
        )
    }
}
