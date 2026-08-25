package no.nav.helse.spesialist.client.spforsikring

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.Scenario
import no.nav.helse.spesialist.application.Folketrygdlovenreferanse
import no.nav.helse.spesialist.application.Forsikringsvurdering
import no.nav.helse.spesialist.application.IndividuellForsikring
import no.nav.helse.spesialist.application.KollektivForsikring
import no.nav.helse.spesialist.application.testfixtures.InMemoryAccessTokenProvider
import no.nav.helse.spesialist.domain.ForsikringsvurderingId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SpForsikringClientForsikringsvurderingHenterTest {
    @Suppress("JUnitMalformedDeclaration")
    @RegisterExtension
    private val wireMock: WireMockExtension =
        WireMockExtension
            .newInstance()
            .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
            .build()

    val forsikringsvurderingId = ForsikringsvurderingId(UUID.randomUUID())

    @Test
    fun `mapper svar som forventet ved mottatt forsikring`() {
        // Given:
        val identitetsnummer = lagIdentitetsnummer()
        val client =
            setupStubAndClient(
                forsikringProxyResponse =
                    okJson(
                        """
                        {
                            "identitetsnummer": "${identitetsnummer.value}",
                            "samletDekning": { "grad": 100, "fraDag": 17 },
                            "kollektivForsikring": {
                                "navn": "100 % fra 17. dag (Kollektiv)",
                                "dekningFolketrygdlovenreferanse": {
                                    "kapittel": 8,
                                    "paragrafIKapittel": 34,
                                    "ledd": 1,
                                    "bokstav": null
                                },
                                "kollektivFolketrygdlovenreferanse": {
                                    "kapittel": 8,
                                    "paragrafIKapittel": 39,
                                    "ledd": null,
                                    "bokstav": null
                                }
                            },
                            "individuelleForsikringer": [
                                {
                                    "navn": "100 % fra 17. dag (Individuell)",
                                    "dekningFolketrygdlovenreferanse": {
                                        "kapittel": 8,
                                        "paragrafIKapittel": 36,
                                        "ledd": 1,
                                        "bokstav": "b"
                                    },
                                    "virkningsdato": "2020-01-01",
                                    "opphørsdato": null,
                                    "konklusjon": {
                                        "forklaring": "Lagt til grunn",
                                        "folketrygdlovenreferanse": null
                                    },
                                    "lagtTilGrunn": true
                                },
                                {
                                    "navn": "80 % fra 1. dag (Individuell)",
                                    "dekningFolketrygdlovenreferanse": {
                                        "kapittel": 8,
                                        "paragrafIKapittel": 36,
                                        "ledd": 1,
                                        "bokstav": "a"
                                    },
                                    "virkningsdato": "2018-01-01",
                                    "opphørsdato": "2019-12-31",
                                    "konklusjon": {
                                        "forklaring": "Forsikringen opphørte før skjæringstidspunktet",
                                        "folketrygdlovenreferanse": {
                                            "kapittel": 8,
                                            "paragrafIKapittel": 37,
                                            "ledd": null,
                                            "bokstav": null
                                        }
                                    },
                                    "lagtTilGrunn": false
                                }
                            ],
                            "vurdertTidspunkt": "2020-02-01T09:31:00Z"
                        }
                        """.trimIndent(),
                    ),
                forsikringsvurderingId = forsikringsvurderingId,
            )

        // When:
        val actualForsikring = client.hent(forsikringsvurderingId = forsikringsvurderingId)

        // Then:
        assertNotNull(actualForsikring)
        assertEquals(identitetsnummer, actualForsikring.identitetsnummer)
        assertEquals(Instant.parse("2020-02-01T09:31:00Z"), actualForsikring.vurdertTidspunkt)
        assertEquals(Forsikringsvurdering.Dekning(grad = 100, fraDag = 17), actualForsikring.samletDekning)
        assertEquals(
            KollektivForsikring(
                navn = "100 % fra 17. dag (Kollektiv)",
                dekningFolketrygdlovenreferanse =
                    Folketrygdlovenreferanse(kapittel = 8, paragrafIKapittel = 34, ledd = 1, bokstav = null),
                kollektivFolketrygdlovenreferanse =
                    Folketrygdlovenreferanse(kapittel = 8, paragrafIKapittel = 39, ledd = null, bokstav = null),
            ),
            actualForsikring.kollektivForsikring,
        )
        assertEquals(
            listOf(
                IndividuellForsikring(
                    navn = "100 % fra 17. dag (Individuell)",
                    dekningFolketrygdlovenreferanse =
                        Folketrygdlovenreferanse(kapittel = 8, paragrafIKapittel = 36, ledd = 1, bokstav = 'b'),
                    virkningsdato = LocalDate.of(2020, 1, 1),
                    opphørsdato = null,
                    konklusjon =
                        IndividuellForsikring.Konklusjon(
                            forklaring = "Lagt til grunn",
                            folketrygdlovenreferanse = null,
                        ),
                    lagtTilGrunn = true,
                ),
                IndividuellForsikring(
                    navn = "80 % fra 1. dag (Individuell)",
                    dekningFolketrygdlovenreferanse =
                        Folketrygdlovenreferanse(kapittel = 8, paragrafIKapittel = 36, ledd = 1, bokstav = 'a'),
                    virkningsdato = LocalDate.of(2018, 1, 1),
                    opphørsdato = LocalDate.of(2019, 12, 31),
                    konklusjon =
                        IndividuellForsikring.Konklusjon(
                            forklaring = "Forsikringen opphørte før skjæringstidspunktet",
                            folketrygdlovenreferanse =
                                Folketrygdlovenreferanse(kapittel = 8, paragrafIKapittel = 37, ledd = null, bokstav = null),
                        ),
                    lagtTilGrunn = false,
                ),
            ),
            actualForsikring.individuelleForsikringer,
        )
    }

    @Test
    fun `mapper svar som forventet ved ingen forsikring`() {
        // Given:
        val identitetsnummer = lagIdentitetsnummer()
        val client =
            setupStubAndClient(
                forsikringProxyResponse =
                    okJson(
                        """
                        {
                            "identitetsnummer": "${identitetsnummer.value}",
                            "samletDekning": null,
                            "kollektivForsikring": null,
                            "individuelleForsikringer": [],
                            "vurdertTidspunkt": "2020-02-01T09:30:00Z"
                        }
                        """.trimIndent(),
                    ),
                forsikringsvurderingId = forsikringsvurderingId,
            )

        // When:
        val actualForsikring = client.hent(forsikringsvurderingId = forsikringsvurderingId)

        // Then:
        assertEquals(
            expected =
                Forsikringsvurdering(
                    identitetsnummer = identitetsnummer,
                    samletDekning = null,
                    kollektivForsikring = null,
                    individuelleForsikringer = emptyList(),
                    vurdertTidspunkt = Instant.parse("2020-02-01T09:30:00Z"),
                ),
            actual = actualForsikring,
        )
    }

    @Test
    fun `feiler om sp-forsikring gir tilbake HTTP 500`() {
        testMedForventningOmFeiletKall(
            stubResponse = WireMock.serverError().withBody("Her står det en feilmelding som ikke engang er JSON"),
            expectedException = RuntimeException("Feil fra forsikringstjeneste: 500"),
            forsikringsvurderingId = forsikringsvurderingId,
        )
    }

    @Test
    fun `gir null om sp-forsikring gir tilbake HTTP 404`() {
        // Given:
        val client =
            setupStubAndClient(
                forsikringProxyResponse = WireMock.notFound().withBody("""{ "feil": "Finnes ikke" }"""),
                forsikringsvurderingId = forsikringsvurderingId,
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
            get("/forsikringsvurderinger/${forsikringsvurderingId.value}")
                .inScenario("scenario")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(WireMock.serverError().withBody("Her står det en feilmelding som ikke engang er JSON"))
                .willSetStateTo("har feilet én gang"),
        )
        wireMock.stubFor(
            get("/forsikringsvurderinger/${forsikringsvurderingId.value}")
                .inScenario("scenario")
                .whenScenarioStateIs("har feilet én gang")
                .willReturn(
                    okJson(
                        """
                        {
                            "identitetsnummer": "${identitetsnummer.value}",
                            "samletDekning": { "grad": 100, "fraDag": 17 },
                            "kollektivForsikring": null,
                            "individuelleForsikringer": [],
                            "vurdertTidspunkt": "2020-02-01T09:30:00Z"
                        }
                        """.trimIndent(),
                    ),
                ),
        )
        val client =
            SpForsikringClientForsikringsvurderingHenter(
                configuration =
                    ClientSpForsikringModule.Configuration(
                        apiUrl = wireMock.runtimeInfo.httpBaseUrl,
                        scope = "scoap",
                    ),
                accessTokenProvider = InMemoryAccessTokenProvider("gief axess plz"),
            )

        // When:
        val actualForsikring = client.hent(forsikringsvurderingId)

        // Then:
        wireMock.verify(2, getRequestedFor(urlEqualTo("/forsikringsvurderinger/${forsikringsvurderingId.value}")))
        assertNotNull(actualForsikring)
        assertEquals(identitetsnummer, actualForsikring.identitetsnummer)
        assertEquals(Forsikringsvurdering.Dekning(grad = 100, fraDag = 17), actualForsikring.samletDekning)
    }

    private fun testMedForventningOmFeiletKall(
        stubResponse: ResponseDefinitionBuilder?,
        forsikringsvurderingId: ForsikringsvurderingId,
        expectedException: Exception,
    ) {
        val client = setupStubAndClient(stubResponse, forsikringsvurderingId)
        val actualException =
            runCatching {
                client.hent(forsikringsvurderingId)
            }.exceptionOrNull()

        assertNotNull(actualException)
        assertEquals(expectedException::class, actualException::class)
        assertEquals(expectedException.message, actualException.message)
    }

    private fun setupStubAndClient(
        forsikringProxyResponse: ResponseDefinitionBuilder?,
        forsikringsvurderingId: ForsikringsvurderingId,
    ): SpForsikringClientForsikringsvurderingHenter {
        wireMock.stubFor(
            get("/forsikringsvurderinger/${forsikringsvurderingId.value}").willReturn(
                forsikringProxyResponse,
            ),
        )

        return SpForsikringClientForsikringsvurderingHenter(
            configuration =
                ClientSpForsikringModule.Configuration(
                    apiUrl = wireMock.runtimeInfo.httpBaseUrl,
                    scope = "scoap",
                ),
            accessTokenProvider = InMemoryAccessTokenProvider("gief axess plz"),
        )
    }
}
