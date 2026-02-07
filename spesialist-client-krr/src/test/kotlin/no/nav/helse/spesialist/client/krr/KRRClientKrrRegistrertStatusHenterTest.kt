package no.nav.helse.spesialist.client.krr

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter.KrrRegistrertStatus.RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class KRRClientKrrRegistrertStatusHenterTest {
    private val identitetsnummer = lagIdentitetsnummer().value

    @Suppress("JUnitMalformedDeclaration")
    @RegisterExtension
    private val wireMock: WireMockExtension =
        WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
            .build()

    @ParameterizedTest(name = "forventer {2} ved (kanVarsles={0}, reservert={1})")
    @CsvSource(
        "false,false,RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING",
        "false,true,RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING",
        "true,false,IKKE_RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING",
        "true,true,RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING"
    )
    fun `mapper svar som forventet`(
        kanVarsles: Boolean,
        reservert: Boolean,
        expectedRegistrertStatus: KrrRegistrertStatusHenter.KrrRegistrertStatus
    ) {
        testMedForventningOmVellykketKall(
            stubResponse = okJson(
                """
                    {
                      "personer": {
                        "$identitetsnummer": {
                          "aktiv": true,
                          "kanVarsles": $kanVarsles,
                          "reservert": $reservert
                        }
                      },
                      "feil": {}
                    }
                """.trimIndent()
            ),
            expectedRegistrertStatus = expectedRegistrertStatus
        )
    }

    @Test
    fun `mapper inaktiv person som at den ikke finnes i KRR`() {
        testMedForventningOmVellykketKall(
            stubResponse = okJson("""{ "personer": { "$identitetsnummer": { "aktiv": false } } }"""),
            expectedRegistrertStatus = KrrRegistrertStatusHenter.KrrRegistrertStatus.IKKE_REGISTRERT_I_KRR
        )
    }

    @Test
    fun `tåler svar med ekstra felter`() {
        testMedForventningOmVellykketKall(
            stubResponse = okJson(
                """
                    {
                      "personer": {
                        "$identitetsnummer": {
                          "aktiv": true,
                          "something-else": "?",
                          "kanVarsles": false,
                          "x": "YZ",
                          "reservert": true
                        }
                      }
                    }
                """.trimIndent()
            ),
            expectedRegistrertStatus = RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING
        )
    }

    @Test
    fun `feiler om ett felt mangler`() {
        testMedForventningOmFeiletKall(
            stubResponse = okJson(
                """
                    {
                      "personer": {
                        "$identitetsnummer": {
                          "aktiv": true,
                          "reservert": true
                        }
                      },
                      "feil": {}
                    }
                """.trimIndent()
            ),
            expectedException = IllegalStateException("Fant ikke boolean-verdi for feltet kanVarsles")
        )
    }

    @Test
    fun `feiler om ett felt har feil type`() {
        testMedForventningOmFeiletKall(
            stubResponse = okJson(
                """
                    {
                      "personer": {
                        "$identitetsnummer": {
                          "aktiv": true,
                          "kanVarsles": "false",
                          "reservert": true
                        }
                      },
                      "feil": {}
                    }
                """.trimIndent()
            ),
            expectedException = IllegalStateException("Fant ikke boolean-verdi for feltet kanVarsles")
        )
    }

    @Test
    fun `feiler om personen ikke dukker opp i svaret`() {
        testMedForventningOmFeiletKall(
            stubResponse = okJson(
                """
                    {
                      "personer": {
                        "${lagIdentitetsnummer().value}": {
                          "aktiv": true,
                          "kanVarsles": true,
                          "reservert": false
                        }
                      },
                      "feil": {}
                    }
                """.trimIndent()
            ),
            expectedException = IllegalStateException("Fant ikke igjen personen i responsen fra KRR")
        )
    }

    @Test
    fun `feiler om feil-objektet inneholder noe`() {
        testMedForventningOmFeiletKall(
            stubResponse = okJson(
                """
                    {
                      "personer": {
                        "$identitetsnummer": {
                          "aktiv": true,
                          "kanVarsles": true,
                          "reservert": false
                        }
                      },
                      "feil": {
                        "noe": "skjedde galt"
                      }
                    }
                """.trimIndent()
            ),
            expectedException = IllegalStateException("Fikk feil tilbake fra KRR")
        )
    }

    @Test
    fun `feiler om KRR gir tilbake HTTP 500`() {
        testMedForventningOmFeiletKall(
            stubResponse = WireMock.serverError().withBody("Her står det en feilmelding som ikke engang er JSON"),
            expectedException = IllegalStateException("Fikk HTTP 500 tilbake fra KRR")
        )
    }

    private fun testMedForventningOmVellykketKall(
        stubResponse: ResponseDefinitionBuilder?,
        expectedRegistrertStatus: KrrRegistrertStatusHenter.KrrRegistrertStatus
    ) {
        // Given:
        val client = setupStubAndClient(stubResponse)

        // When:
        val actualReservasjon = client.hentForPerson(identitetsnummer)

        // Then:
        assertEquals(expectedRegistrertStatus, actualReservasjon)
    }

    private fun testMedForventningOmFeiletKall(
        stubResponse: ResponseDefinitionBuilder?,
        expectedException: Exception
    ) {
        // Given:
        val client = setupStubAndClient(stubResponse)

        // When:
        val actualException = runCatching {
            client.hentForPerson(identitetsnummer)
        }.exceptionOrNull()

        // Then:
        assertNotNull(actualException)
        assertEquals(expectedException::class, actualException::class)
        assertEquals(expectedException.message, actualException.message)
    }

    private fun setupStubAndClient(krrProxyResponse: ResponseDefinitionBuilder?): KRRClientKrrRegistrertStatusHenter {
        wireMock.stubFor(post("/rest/v1/personer").willReturn(krrProxyResponse))

        return KRRClientKrrRegistrertStatusHenter(
            configuration = ClientKrrModule.Configuration.Client(
                apiUrl = wireMock.runtimeInfo.httpBaseUrl,
                scope = "scoap"
            ),
            accessTokenGenerator = { "gief axess plz" }
        )
    }

}
