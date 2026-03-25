package no.nav.helse.spesialist.client.sparkel.sykepengeperioder

import com.fasterxml.jackson.core.type.TypeReference
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import no.nav.helse.spesialist.application.Cache
import no.nav.helse.spesialist.domain.Infotrygdperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import org.junit.jupiter.api.extension.RegisterExtension
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SparkelSykepengeperioderClientTest {
    private val identitetsnummer = lagIdentitetsnummer()
    private val fom = LocalDate.of(2024, 1, 1)

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
                      "utbetaltePerioder": [
                        {
                          "personidentifikator": "${identitetsnummer.value}",
                          "organisasjonsnummer": "123456789",
                          "fom": "2024-01-01",
                          "tom": "2024-01-31",
                          "grad": 80,
                          "dagsats": 1000.00,
                          "type": "UTBETALING",
                          "tags": []
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
            expectedPerioder =
                listOf(
                    Infotrygdperiode(
                        personidentifikator = identitetsnummer,
                        organisasjonsnummer = "123456789",
                        fom = LocalDate.of(2024, 1, 1),
                        tom = LocalDate.of(2024, 1, 31),
                        grad = 80,
                        dagsats = BigDecimal("1000.00"),
                        type = "UTBETALING",
                        tags = emptySet(),
                    ),
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
                      "utbetaltePerioder": [
                        {
                          "personidentifikator": "${identitetsnummer.value}",
                          "organisasjonsnummer": null,
                          "fom": "2024-02-01",
                          "tom": "2024-02-29",
                          "grad": 100,
                          "dagsats": 2000.00,
                          "type": "ETTERBETALING",
                          "tags": ["UsikkerGrad"],
                          "ekstraFelt": "ignoreres"
                        }
                      ],
                      "etAnnetEkstraFelt": true
                    }
                    """.trimIndent(),
                ),
            expectedPerioder =
                listOf(
                    Infotrygdperiode(
                        personidentifikator = identitetsnummer,
                        organisasjonsnummer = null,
                        fom = LocalDate.of(2024, 2, 1),
                        tom = LocalDate.of(2024, 2, 29),
                        grad = 100,
                        dagsats = BigDecimal("2000.00"),
                        type = "ETTERBETALING",
                        tags = setOf("UsikkerGrad"),
                    ),
                ),
        )
    }

    @Test
    fun `returnerer tom liste når ingen perioder finnes`() {
        testMedForventningOmVellykketKall(
            stubResponse = okJson("""{ "utbetaltePerioder": [] }"""),
            expectedPerioder = emptyList(),
        )
    }

    @Test
    fun `feiler om sparkel-sykepengeperioder gir tilbake HTTP 500`() {
        testMedForventningOmFeiletKall(
            stubResponse = WireMock.serverError().withBody("Her er en feilmelding"),
            expectedException = IllegalStateException("Fikk HTTP 500 tilbake fra sparkel-sykepengeperioder"),
        )
    }

    @Test
    fun `feiler om sparkel-sykepengeperioder gir tilbake HTTP 401`() {
        testMedForventningOmFeiletKall(
            stubResponse = WireMock.unauthorized(),
            expectedException = IllegalStateException("Fikk HTTP 401 tilbake fra sparkel-sykepengeperioder"),
        )
    }

    private fun testMedForventningOmVellykketKall(
        stubResponse: ResponseDefinitionBuilder,
        expectedPerioder: List<Infotrygdperiode>,
    ) {
        val client = setupStubAndClient(stubResponse)
        val actualPerioder = client.hentFor(identitetsnummer, fom)
        assertEquals(expectedPerioder, actualPerioder)
    }

    private fun testMedForventningOmFeiletKall(
        stubResponse: ResponseDefinitionBuilder,
        expectedException: Exception,
    ) {
        val client = setupStubAndClient(stubResponse)

        val actualException =
            runCatching {
                client.hentFor(identitetsnummer, fom)
            }.exceptionOrNull()

        assertNotNull(actualException)
        assertEquals(expectedException::class, actualException::class)
        assertEquals(expectedException.message, actualException.message)
    }

    private fun setupStubAndClient(sparkelSykepengeperioderResponse: ResponseDefinitionBuilder): SparkelSykepengeperioderClient {
        wireMock.stubFor(post("/").willReturn(sparkelSykepengeperioderResponse))

        return SparkelSykepengeperioderClient(
            configuration =
                ClientSparkelSykepengeperioderModule.Configuration(
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
