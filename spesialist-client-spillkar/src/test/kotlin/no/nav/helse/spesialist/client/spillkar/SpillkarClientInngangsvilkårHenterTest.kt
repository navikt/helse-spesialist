package no.nav.helse.spesialist.client.spillkar

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import no.nav.helse.spesialist.application.spillkar.AutomatiskVurdering
import no.nav.helse.spesialist.application.spillkar.`VurdertInngangsvilkår`
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SpillkarClientInngangsvilkårHenterTest {
    @Suppress("JUnitMalformedDeclaration")
    @RegisterExtension
    private val wireMock: WireMockExtension =
        WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build()

    private val skjæringstidspunkt = LocalDate.of(2021, 1, 1)
    private val personidentifikatorer = listOf("11111111111")

    @Test
    fun `mapper svar med manuell vurdering korrekt`() {
        val samlingId = UUID.randomUUID()
        val tidspunkt = "2024-01-15T10:00:00"
        setupStub(
            okJson(
                """
                {
                  "samlingAvVurderteInngangsvilkår": [
                    {
                      "samlingAvVurderteInngangsvilkårId": "$samlingId",
                      "versjon": 1,
                      "skjæringstidspunkt": "2021-01-01",
                      "vurderteInngangsvilkår": [
                        {
                          "vilkårskode": "MEDLEMSKAP",
                          "vurderingskode": "MEDLEMSKAP_JA",
                          "tidspunkt": "$tidspunkt",
                          "manuellVurdering": {
                            "navident": "P31337",
                            "begrunnelse": "Dokumentasjon foreligger"
                          }
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val result = lagKlient().hentInngangsvilkår(personidentifikatorer, skjæringstidspunkt)

        assertEquals(1, result.size)
        val samling = result[0]
        assertEquals(samlingId, samling.samlingAvVurderteInngangsvilkårId)
        assertEquals(1, samling.versjon)
        assertEquals(skjæringstidspunkt, samling.skjæringstidspunkt)
        assertEquals(1, samling.vurderteInngangsvilkår.size)
        val vurdering = assertIs<VurdertInngangsvilkår.ManueltVurdertInngangsvilkår>(samling.vurderteInngangsvilkår[0])
        assertEquals("MEDLEMSKAP", vurdering.vilkårskode)
        assertEquals("MEDLEMSKAP_JA", vurdering.vurderingskode)
        assertEquals(LocalDateTime.parse(tidspunkt), vurdering.tidspunkt)
        assertEquals("P31337", vurdering.navident)
        assertEquals("Dokumentasjon foreligger", vurdering.begrunnelse)
    }

    @Test
    fun `mapper svar med automatisk vurdering korrekt`() {
        val samlingId = UUID.randomUUID()
        val tidspunkt = "2024-01-15T10:00:00"
        val grunnlagsdataId = UUID.randomUUID()
        setupStub(
            okJson(
                """
                {
                  "samlingAvVurderteInngangsvilkår": [
                    {
                      "samlingAvVurderteInngangsvilkårId": "$samlingId",
                      "versjon": 2,
                      "skjæringstidspunkt": "2021-01-01",
                      "vurderteInngangsvilkår": [
                        {
                          "vilkårskode": "ALDER",
                          "vurderingskode": "ALDER_OK",
                          "tidspunkt": "$tidspunkt",
                          "automatiskVurdering": {
                            "system": "Spleis",
                            "versjon": "2026.02.19-10.07-793bcfe",
                            "grunnlagsdata": {
                              "hei": "$grunnlagsdataId"
                             }
                          }
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val result = lagKlient().hentInngangsvilkår(personidentifikatorer, skjæringstidspunkt)

        val vurdering = assertIs<VurdertInngangsvilkår.AutomatiskVurdertInngangsvilkår>(result[0].vurderteInngangsvilkår[0])
        assertEquals("ALDER", vurdering.vilkårskode)
        assertEquals("ALDER_OK", vurdering.vurderingskode)
        assertEquals(AutomatiskVurdering(system = "Spleis", versjon = "2026.02.19-10.07-793bcfe", grunnlagsdata = mapOf("hei" to grunnlagsdataId.toString())), vurdering.automatiskVurdering)
    }

    @Test
    fun `returnerer tom liste ved tomt svar`() {
        setupStub(
            okJson("""{ "samlingAvVurderteInngangsvilkår": [] }""")
        )

        val result = lagKlient().hentInngangsvilkår(personidentifikatorer, skjæringstidspunkt)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `feiler ved HTTP 500`() {
        setupStub(serverError().withBody("Intern feil"))

        val exception = runCatching {
            lagKlient().hentInngangsvilkår(personidentifikatorer, skjæringstidspunkt)
        }.exceptionOrNull()

        assertNotNull(exception)
        assertIs<RuntimeException>(exception)
    }

    private fun setupStub(response: ResponseDefinitionBuilder) {
        wireMock.stubFor(post(urlEqualTo("/vurderte-inngangsvilkar/alle")).willReturn(response))
    }

    private fun lagKlient() =
        SpillkarClientInngangsvilkårHenter(
            configuration =
                ClientSpillkarModule.Configuration(
                    apiUrl = wireMock.runtimeInfo.httpBaseUrl,
                    scope = "scoap",
                ),
            accessTokenGenerator = { "test-token" },
        )
}
