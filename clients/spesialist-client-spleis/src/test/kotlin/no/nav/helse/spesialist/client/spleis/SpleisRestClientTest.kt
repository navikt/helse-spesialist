package no.nav.helse.spesialist.client.spleis

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.notFound
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import no.nav.helse.spesialist.application.snapshot.SnapshotBeregnetPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotInfotrygdVilkarsgrunnlag
import no.nav.helse.spesialist.application.snapshot.SnapshotInntektsmelding
import no.nav.helse.spesialist.application.snapshot.SnapshotSoknadNav
import no.nav.helse.spesialist.application.snapshot.SnapshotSpleisVilkarsgrunnlag
import no.nav.helse.spesialist.application.snapshot.SnapshotUberegnetPeriode
import no.nav.helse.spesialist.application.testfixtures.InMemoryAccessTokenProvider
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SpleisRestClientTest {
    @Suppress("JUnitMalformedDeclaration")
    @RegisterExtension
    private val wireMock: WireMockExtension =
        WireMockExtension
            .newInstance()
            .options(wireMockConfig().dynamicPort())
            .build()

    @Test
    fun `returnerer person ved vellykket svar`() {
        setupStub(
            okJson(
                """
                {
                  "aktorId": "1234567890123",
                  "fodselsnummer": "11111111111",
                  "arbeidsgivere": [],
                  "dodsdato": null,
                  "versjon": 1,
                  "vilkarsgrunnlag": []
                }
                """.trimIndent(),
            ),
        )

        val result = lagKlient().hentPerson("11111111111")

        assertNotNull(result)
        assertEquals("1234567890123", result.aktorId)
        assertEquals("11111111111", result.fodselsnummer)
        assertEquals(1, result.versjon)
    }

    @Test
    fun `deserialiserer og mapper realistisk respons med alle undertyper av periode, hendelse og vilkarsgrunnlag`() {
        setupStub(okJson(REALISTISK_PERSON_RESPONS))

        val person = lagKlient().hentPerson("11111111111")

        assertNotNull(person)
        val snapshotPerson = person.tilSnapshotPerson()

        val perioder =
            snapshotPerson.arbeidsgivere
                .single()
                .behandlinger
                .single()
                .perioder
        assertEquals(2, perioder.size)
        val uberegnetPeriode = assertIs<SnapshotUberegnetPeriode>(perioder[0])
        val beregnetPeriode = assertIs<SnapshotBeregnetPeriode>(perioder[1])

        val hendelser = uberegnetPeriode.hendelser
        assertEquals(2, hendelser.size)
        assertIs<SnapshotInntektsmelding>(hendelser[0])
        assertIs<SnapshotSoknadNav>(hendelser[1])

        assertEquals("AG123", beregnetPeriode.utbetaling.arbeidsgiverFagsystemId)

        val vilkarsgrunnlag = snapshotPerson.vilkarsgrunnlag
        assertEquals(2, vilkarsgrunnlag.size)
        assertIs<SnapshotSpleisVilkarsgrunnlag>(vilkarsgrunnlag[0])
        assertIs<SnapshotInfotrygdVilkarsgrunnlag>(vilkarsgrunnlag[1])
    }

    @Test
    fun `feiler ikke ved ukjente felter i responsen (fremtidssikring mot nye felter fra spleis)`() {
        setupStub(
            okJson(
                """
                {
                  "aktorId": "1234567890123",
                  "fodselsnummer": "11111111111",
                  "arbeidsgivere": [],
                  "dodsdato": null,
                  "versjon": 1,
                  "vilkarsgrunnlag": [],
                  "etHeltNyttFeltViIkkeKjennerTilEnna": "noe verdi",
                  "enNyStruktur": { "med": ["nestede", "verdier"] }
                }
                """.trimIndent(),
            ),
        )

        val result = lagKlient().hentPerson("11111111111")

        assertNotNull(result)
        assertEquals("1234567890123", result.aktorId)
        assertEquals("11111111111", result.fodselsnummer)
    }

    @Test
    fun `returnerer null ved 404`() {
        setupStub(notFound())

        val result = lagKlient().hentPerson("11111111111")

        assertNull(result)
    }

    @Test
    fun `feiler ved HTTP 500`() {
        setupStub(serverError().withBody("Intern feil"))

        val exception =
            runCatching {
                lagKlient().hentPerson("11111111111")
            }.exceptionOrNull()

        assertNotNull(exception)
        assertIs<IllegalStateException>(exception)
    }

    private fun setupStub(response: ResponseDefinitionBuilder) {
        wireMock.stubFor(post(urlEqualTo("/api/person")).willReturn(response))
    }

    private fun lagKlient() =
        SpleisRestClient(
            accessTokenProvider = InMemoryAccessTokenProvider("test-token"),
            spleisUrl = URI.create(wireMock.runtimeInfo.httpBaseUrl),
            spleisClientId = "local-app",
        )

    private companion object {
        // Dekker undertypene av Tidslinjeperiode (UberegnetPeriode/BeregnetPeriode),
        // Hendelse (Inntektsmelding/SendtSoknadNav) og Vilkarsgrunnlag
        // (SpleisVilkarsgrunnlag/InfotrygdVilkarsgrunnlag) — se JsonSubTypes-diskriminatorene i
        // Tidslinjeperiode.kt, Hendelse.kt og Vilkarsgrunnlag.kt.
        @Language("JSON")
        private val REALISTISK_PERSON_RESPONS =
            """
            {
              "aktorId": "1234567890123",
              "fodselsnummer": "11111111111",
              "dodsdato": null,
              "versjon": 1,
              "arbeidsgivere": [
                {
                  "organisasjonsnummer": "987654321",
                  "ghostPerioder": [],
                  "generasjoner": [
                    {
                      "id": "00000000-0000-0000-0000-000000000010",
                      "kildeTilGenerasjon": "00000000-0000-0000-0000-000000000010",
                      "perioder": [
                        {
                          "type": "UberegnetPeriode",
                          "behandlingId": "00000000-0000-0000-0000-000000000001",
                          "kilde": "00000000-0000-0000-0000-000000000001",
                          "fom": "2023-01-01",
                          "tom": "2023-01-31",
                          "tidslinje": [
                            {
                              "dato": "2023-01-01",
                              "sykdomsdagtype": "Sykedag",
                              "utbetalingsdagtype": "NavDag",
                              "kilde": { "id": "00000000-0000-0000-0000-000000000020", "type": "Sykmelding" },
                              "grad": 100.0,
                              "utbetalingsinfo": null,
                              "begrunnelser": []
                            }
                          ],
                          "periodetype": "Forstegangsbehandling",
                          "erForkastet": false,
                          "opprettet": "2023-01-01T00:00:00",
                          "vedtaksperiodeId": "00000000-0000-0000-0000-000000000002",
                          "periodetilstand": "TilGodkjenning",
                          "skjaeringstidspunkt": "2023-01-01",
                          "hendelser": [
                            {
                              "type": "Inntektsmelding",
                              "id": "im-1",
                              "eksternDokumentId": "im-1-ekstern",
                              "mottattDato": "2023-01-01T00:00:00",
                              "beregnetInntekt": 500000.0
                            },
                            {
                              "type": "SendtSoknadNav",
                              "id": "sn-1",
                              "eksternDokumentId": "sn-1-ekstern",
                              "fom": "2023-01-01",
                              "tom": "2023-01-31",
                              "rapportertDato": "2023-01-31T00:00:00",
                              "sendtNav": "2023-01-31T00:00:00"
                            }
                          ],
                          "pensjonsgivendeInntekter": [],
                          "inntektstype": "EnArbeidsgiver"
                        },
                        {
                          "type": "BeregnetPeriode",
                          "behandlingId": "00000000-0000-0000-0000-000000000001",
                          "kilde": "00000000-0000-0000-0000-000000000001",
                          "fom": "2023-02-01",
                          "tom": "2023-02-28",
                          "tidslinje": [
                            {
                              "dato": "2023-02-01",
                              "sykdomsdagtype": "Sykedag",
                              "utbetalingsdagtype": "NavDag",
                              "kilde": { "id": "00000000-0000-0000-0000-000000000020", "type": "Sykmelding" },
                              "grad": 100.0,
                              "utbetalingsinfo": null,
                              "begrunnelser": []
                            }
                          ],
                          "periodetype": "Forlengelse",
                          "erForkastet": false,
                          "opprettet": "2023-02-01T00:00:00",
                          "vedtaksperiodeId": "00000000-0000-0000-0000-000000000002",
                          "periodetilstand": "Utbetalt",
                          "skjaeringstidspunkt": "2023-01-01",
                          "hendelser": [],
                          "pensjonsgivendeInntekter": [],
                          "beregningId": "00000000-0000-0000-0000-000000000030",
                          "gjenstaendeSykedager": 200,
                          "forbrukteSykedager": 48,
                          "maksdato": "2025-01-01",
                          "utbetaling": {
                            "id": "00000000-0000-0000-0000-000000000030",
                            "typeEnum": "UTBETALING",
                            "statusEnum": "Utbetalt",
                            "arbeidsgiverNettoBelop": 10000,
                            "personNettoBelop": 0,
                            "arbeidsgiverFagsystemId": "AG123",
                            "personFagsystemId": "P123",
                            "arbeidsgiveroppdrag": null,
                            "personoppdrag": null,
                            "vurdering": null
                          },
                          "periodevilkar": {
                            "sykepengedager": {
                              "skjaeringstidspunkt": "2023-01-01",
                              "maksdato": "2025-01-01",
                              "forbrukteSykedager": 48,
                              "gjenstaendeSykedager": 200,
                              "oppfylt": true
                            },
                            "alder": { "alderSisteSykedag": 40, "oppfylt": true }
                          },
                          "vilkarsgrunnlagId": "00000000-0000-0000-0000-000000000003",
                          "annulleringskandidater": [],
                          "inntektstype": "EnArbeidsgiver"
                        }
                      ]
                    }
                  ]
                }
              ],
              "vilkarsgrunnlag": [
                {
                  "type": "SpleisVilkarsgrunnlag",
                  "id": "00000000-0000-0000-0000-000000000003",
                  "skjaeringstidspunkt": "2023-01-01",
                  "omregnetArsinntekt": 500000.0,
                  "sykepengegrunnlag": 500000.0,
                  "inntekter": [],
                  "arbeidsgiverrefusjoner": [],
                  "beregningsgrunnlag": 500000.0,
                  "grunnbelop": 118620,
                  "sykepengegrunnlagsgrense": {
                    "grunnbelop": 118620,
                    "grense": 711720,
                    "virkningstidspunkt": "2023-05-01"
                  },
                  "antallOpptjeningsdagerErMinst": 28,
                  "opptjeningFra": "2022-12-01",
                  "oppfyllerKravOmMinstelonn": true,
                  "oppfyllerKravOmOpptjening": true,
                  "oppfyllerKravOmMedlemskap": true,
                  "forsikringsvurderingId": null,
                  "opptjeningsvurderingId": "00000000-0000-0000-0000-000000000005"
                },
                {
                  "type": "InfotrygdVilkarsgrunnlag",
                  "id": "00000000-0000-0000-0000-000000000004",
                  "skjaeringstidspunkt": "2022-01-01",
                  "omregnetArsinntekt": 400000.0,
                  "sykepengegrunnlag": 400000.0,
                  "inntekter": [],
                  "arbeidsgiverrefusjoner": [],
                  "opptjeningsvurderingId": "00000000-0000-0000-0000-000000000005"
                }
              ]
            }
            """.trimIndent()
    }
}
