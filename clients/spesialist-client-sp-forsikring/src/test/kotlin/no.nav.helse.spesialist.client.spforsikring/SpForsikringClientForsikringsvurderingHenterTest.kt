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
import no.nav.helse.spesialist.application.Ekskluderingsbegrunnelse
import no.nav.helse.spesialist.application.Ekskluderingsårsak
import no.nav.helse.spesialist.application.Folketrygdlovenreferanse
import no.nav.helse.spesialist.application.Forsikringsvurdering
import no.nav.helse.spesialist.application.testfixtures.InMemoryAccessTokenProvider
import no.nav.helse.spesialist.domain.ForsikringsvurderingId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
                            "harForsikring": true,
                            "dekning": { "grad": 100, "fraDag": 17 },
                            "ekskluderteForsikringer": [
                                {
                                    "virkningsdato": "2018-01-01",
                                    "opphørsdato": "2019-12-31",
                                    "dekningsgrad": 80,
                                    "dekningIVentetid": false,
                                    "navn": "80 % fra dag 1",
                                    "folketrygdlovenreferanse": {
                                        "kapittel": 8,
                                        "paragrafIKapittel": 36,
                                        "ledd": 1,
                                        "bokstav": "a"
                                    },
                                    "ekskluderingsårsak": "OPPHØRT_PÅ_SKJÆRINGSTIDSPUNKT",
                                    "ekskluderingsbegrunnelse": {
                                        "forklaring": "Forsikringen var opphørt på skjæringstidspunktet",
                                        "folketrygdlovenreferanse": {
                                            "kapittel": 8,
                                            "paragrafIKapittel": 37,
                                            "ledd": null,
                                            "bokstav": null
                                        }
                                    }
                                },
                                {
                                    "virkningsdato": "2021-03-01",
                                    "opphørsdato": null,
                                    "dekningsgrad": 100,
                                    "dekningIVentetid": true,
                                    "navn": "100 % fra dag 1",
                                    "folketrygdlovenreferanse": {
                                        "kapittel": 8,
                                        "paragrafIKapittel": 36,
                                        "ledd": 1,
                                        "bokstav": "c"
                                    },
                                    "ekskluderingsårsak": "SKJÆRINGSTIDSPUNKT_INNEN_28_DAGER_FØR_VIRKNINGSDATO",
                                    "ekskluderingsbegrunnelse": {
                                        "forklaring": "Forsikringen var ikke ennå gyldig på skjæringstidspunktet",
                                        "folketrygdlovenreferanse": null
                                    }
                                }
                            ],
                            "gjeldendeForsikring": {
                                "virkningsdato": "2020-01-01",
                                "opphørsdato": null,
                                "dekningsgrad": 100,
                                "dekningIVentetid": false,
                                "navn": "100 % fra dag 17",
                                "folketrygdlovenreferanse": {
                                    "kapittel": 8,
                                    "paragrafIKapittel": 36,
                                    "ledd": 1,
                                    "bokstav": "b"
                                }
                            }
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
        assertTrue(actualForsikring.harForsikring)
        assertEquals(Forsikringsvurdering.Dekning(grad = 100, fraDag = 17), actualForsikring.dekning)

        val gjeldendeForsikring = assertNotNull(actualForsikring.gjeldendeForsikring)
        assertEquals(LocalDate.of(2020, 1, 1), gjeldendeForsikring.virkningsdato)
        assertNull(gjeldendeForsikring.opphørsdato)
        assertEquals(100, gjeldendeForsikring.dekningsgrad)
        assertFalse(gjeldendeForsikring.dekningIVentetid)
        assertEquals("100 % fra dag 17", gjeldendeForsikring.navn)
        assertEquals(
            Folketrygdlovenreferanse(kapittel = 8, paragrafIKapittel = 36, ledd = 1, bokstav = 'b'),
            gjeldendeForsikring.folketrygdlovenreferanse,
        )

        assertEquals(2, actualForsikring.ekskluderteForsikringer.size)
        val (første, andre) = actualForsikring.ekskluderteForsikringer
        assertEquals(LocalDate.of(2018, 1, 1), første.virkningsdato)
        assertEquals(LocalDate.of(2019, 12, 31), første.opphørsdato)
        assertEquals(80, første.dekningsgrad)
        assertEquals(false, første.dekningIVentetid)
        assertEquals(Ekskluderingsårsak.OPPHØRT_PÅ_SKJÆRINGSTIDSPUNKT, første.ekskluderingsårsak)
        assertEquals("80 % fra dag 1", første.navn)
        assertEquals(
            Folketrygdlovenreferanse(kapittel = 8, paragrafIKapittel = 36, ledd = 1, bokstav = 'a'),
            første.folketrygdlovenreferanse,
        )
        assertEquals(
            Ekskluderingsbegrunnelse(
                forklaring = "Forsikringen var opphørt på skjæringstidspunktet",
                folketrygdlovenreferanse =
                    Folketrygdlovenreferanse(kapittel = 8, paragrafIKapittel = 37, ledd = null, bokstav = null),
            ),
            første.ekskluderingsbegrunnelse,
        )
        assertEquals(LocalDate.of(2021, 3, 1), andre.virkningsdato)
        assertNull(andre.opphørsdato)
        assertEquals(100, andre.dekningsgrad)
        assertEquals(true, andre.dekningIVentetid)
        assertEquals(
            Ekskluderingsårsak.SKJÆRINGSTIDSPUNKT_INNEN_28_DAGER_FØR_VIRKNINGSDATO,
            andre.ekskluderingsårsak,
        )
        assertEquals("100 % fra dag 1", andre.navn)
        assertEquals(
            Folketrygdlovenreferanse(kapittel = 8, paragrafIKapittel = 36, ledd = 1, bokstav = 'c'),
            andre.folketrygdlovenreferanse,
        )
        assertEquals(
            Ekskluderingsbegrunnelse(
                forklaring = "Forsikringen var ikke ennå gyldig på skjæringstidspunktet",
                folketrygdlovenreferanse = null,
            ),
            andre.ekskluderingsbegrunnelse,
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
                            "harForsikring": false,
                            "dekning": null,
                            "ekskluderteForsikringer": [],
                            "gjeldendeForsikring": null
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
                    harForsikring = false,
                    dekning = null,
                    ekskluderteForsikringer = emptyList(),
                    gjeldendeForsikring = null,
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
                            "harForsikring": true,
                            "dekning": { "grad": 100, "fraDag": 17 },
                            "ekskluderteForsikringer": [],
                            "gjeldendeForsikring": {
                                "virkningsdato": "2020-01-01",
                                "opphørsdato": null,
                                "dekningsgrad": 100,
                                "dekningIVentetid": false,
                                "navn": "100 % fra dag 17",
                                "folketrygdlovenreferanse": {
                                    "kapittel": 8,
                                    "paragrafIKapittel": 36,
                                    "ledd": 1,
                                    "bokstav": "b"
                                }
                            }
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
        assertTrue(actualForsikring.harForsikring)
        assertEquals(Forsikringsvurdering.Dekning(grad = 100, fraDag = 17), actualForsikring.dekning)
        assertEquals(emptyList(), actualForsikring.ekskluderteForsikringer)
        assertEquals(LocalDate.of(2020, 1, 1), assertNotNull(actualForsikring.gjeldendeForsikring).virkningsdato)
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
