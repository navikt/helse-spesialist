package no.nav.helse.spesialist.api.graphql.query

import io.mockk.every
import io.mockk.verify
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.graphql.schema.Soknadstype
import no.nav.helse.spesialist.api.objectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class DokumentQueryTest : AbstractGraphQLApiTest() {
    override val useGraphQLServerWithSeparateMocks = true

    @Test
    fun `Får 400 dersom man gjør oppslag uten dokumentId`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val dokument = runQuery(
            """
            {
                hentSoknad(
                    dokumentId: ""
                    fnr: "$FØDSELSNUMMER"
                ) {
                    sykmeldingSkrevet
                }
            }
        """
        )["errors"].first()

        assertEquals(400, dokument["extensions"]["code"].asInt())
    }

    @Test
    fun `Får 403 dersom man gjør oppslag uten tilgang til person`() {
        val dokument = runQuery(
            """
            {
                hentSoknad(
                    dokumentId: "${UUID.randomUUID()}"
                    fnr: "$FØDSELSNUMMER"
                ) {
                    sykmeldingSkrevet
                }
            }
        """
        )["errors"].first()

        assertEquals(403, dokument["extensions"]["code"].asInt())
    }

    @Test
    fun `Får 408 dersom man ikke har fått søknaden etter 30 retries`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        every { dokumenthåndterer.håndter(any(), any(), any()) } returns objectMapper.createObjectNode().put("error", 408)
        val dokument = runQuery(
            """
            {
                hentSoknad(
                    dokumentId: "${UUID.randomUUID()}"
                    fnr: "$FØDSELSNUMMER"
                ) {
                    sykmeldingSkrevet
                }
            }
        """
        )["errors"].first()

        assertEquals(408, dokument["extensions"]["code"].asInt())
    }

    @Test
    fun `Får 417 dersom man ikke har dokumentet`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        every { dokumenthåndterer.håndter(any(), any(), any()) } returns objectMapper.createObjectNode()
        val dokument = runQuery(
            """
            {
                hentSoknad(
                    dokumentId: "${UUID.randomUUID()}"
                    fnr: "$FØDSELSNUMMER"
                ) {
                    sykmeldingSkrevet
                }
            }
        """
        )["errors"].first()

        assertEquals(417, dokument["extensions"]["code"].asInt())
    }

    @Test
    fun `Får 404 dersom man ikke har fått dokumentet`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        every { dokumenthåndterer.håndter(any(), any(), any()) } returns objectMapper.createObjectNode().put("error", 404)
        val dokument = runQuery(
            """
            {
                hentInntektsmelding(
                    dokumentId: "${UUID.randomUUID()}"
                    fnr: "$FØDSELSNUMMER"
                ) {
                    foersteFravaersdag
                }
            }
        """
        )["errors"].first()

        assertEquals(404, dokument["extensions"]["code"].asInt())
    }

    @Test
    fun `hentSoknad query med riktige tilganger og paramtetre returnerer søknad`() {
        val dokumentId = UUID.randomUUID()
        val arbeidGjenopptatt = LocalDate.now().toString()
        val sykmeldingSkrevet = LocalDateTime.now().toString()


        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        every { dokumenthåndterer.håndter(any(), any(), any()) } returns objectMapper.readTree(
            søknadJsonMedNeiSvar(
                arbeidGjenopptatt,
                sykmeldingSkrevet
            )
        )
        val dokument = runQuery(
            """
            {
                hentSoknad(
                    dokumentId: "$dokumentId"
                    fnr: "$FØDSELSNUMMER"
                ) {
                    type, sykmeldingSkrevet, arbeidGjenopptatt, egenmeldingsdagerFraSykmelding, soknadsperioder {
                    fom, tom, grad, faktiskGrad, sykmeldingsgrad
                    }, sporsmal {
                        tag, sporsmalstekst, undertekst, svartype, svar {verdi}, undersporsmal { 
                            tag, sporsmalstekst, undertekst, svartype, svar {verdi}, undersporsmal { 
                                tag, sporsmalstekst, undertekst, svartype, svar {verdi}, undersporsmal {
                                    tag, sporsmalstekst, undertekst, svartype, svar {verdi}, undersporsmal {
                                        tag, sporsmalstekst, undertekst, svartype, svar {verdi}
                                    }
                                }
                            }    
                        }
                    }
                }
            }
        """
        )["data"]["hentSoknad"]

        verify(exactly = 1) {
            dokumenthåndterer.håndter(
                fødselsnummer = FØDSELSNUMMER,
                dokumentId = dokumentId,
                dokumentType = DokumentType.SØKNAD.name
            )
        }

        assertEquals(6, dokument.size())
        assertEquals(Soknadstype.Arbeidstaker.name, dokument["type"]?.asText())
        assertEquals(arbeidGjenopptatt, dokument["arbeidGjenopptatt"].asText())
        assertEquals(sykmeldingSkrevet, dokument["sykmeldingSkrevet"].asText())
        assertEquals("2018-01-01", dokument["egenmeldingsdagerFraSykmelding"].first().asText())
        val hentetSoknadsperioder = dokument["soknadsperioder"].single()
        assertEquals("2018-01-01", hentetSoknadsperioder["fom"].asText())
        assertEquals("2018-01-31", hentetSoknadsperioder["tom"].asText())
        assertEquals(100, hentetSoknadsperioder["grad"].asInt())
        assertEquals(100, hentetSoknadsperioder["sykmeldingsgrad"].asInt())
        assertTrue(hentetSoknadsperioder["faktiskGrad"].isNull)
        val spørsmål = dokument["sporsmal"]
        assertEquals(0, spørsmål.size())
        assertTrue(spørsmål.none { it["tag"].asText() == "BEKREFT_OPPLYSNINGER" })
        assertTrue(spørsmål.none { it["tag"].asText() == "ANSVARSERKLARING" })
        assertTrue(spørsmål.none { it["tag"].asText() == "VAER_KLAR_OVER_AT" })
    }

    @Test
    fun `hentSoknad query returnerer kun spørsmål vi er interessert i å vise`() {
        val dokumentId = UUID.randomUUID()

        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        every { dokumenthåndterer.håndter(any(), any(), any()) } returns objectMapper.readTree(
            søknadJsonMedJaSvar()
        )
        val dokument = runQuery(
            """
            {
                hentSoknad(
                    dokumentId: "$dokumentId"
                    fnr: "$FØDSELSNUMMER"
                ) {
                    type, sykmeldingSkrevet, arbeidGjenopptatt, egenmeldingsdagerFraSykmelding, soknadsperioder {
                    fom, tom, grad, faktiskGrad, sykmeldingsgrad
                    }, sporsmal {
                        tag, sporsmalstekst, undertekst, svartype, svar {verdi}, undersporsmal { 
                            tag, sporsmalstekst, undertekst, svartype, svar {verdi}, undersporsmal { 
                                tag, sporsmalstekst, undertekst, svartype, svar {verdi}, undersporsmal {
                                    tag, sporsmalstekst, undertekst, svartype, svar {verdi}, undersporsmal {
                                        tag, sporsmalstekst, undertekst, svartype, svar {verdi}, undersporsmal {
                                            tag, sporsmalstekst, undertekst, svartype, svar {verdi}, undersporsmal {
                                                tag, sporsmalstekst, undertekst, svartype, svar {verdi}
                                            }
                                        }
                                    }
                                }
                            }    
                        }
                    }
                }
            }
        """
        )["data"]["hentSoknad"]

        verify(exactly = 1) {
            dokumenthåndterer.håndter(
                fødselsnummer = FØDSELSNUMMER,
                dokumentId = dokumentId,
                dokumentType = DokumentType.SØKNAD.name
            )
        }

        val spørsmål = dokument["sporsmal"]
        assertEquals(6, spørsmål.size())
        assertTrue(spørsmål.none { it["tag"].asText() == "BEKREFT_OPPLYSNINGER" })
        assertTrue(spørsmål.none { it["tag"].asText() == "ANSVARSERKLARING" })
        assertTrue(spørsmål.none { it["tag"].asText() == "VAER_KLAR_OVER_AT" })
        val tilbakeIArbeid = spørsmål.first { it["tag"].asText() == "TILBAKE_I_ARBEID" }
        assertEquals("JA", tilbakeIArbeid["svar"].first()["verdi"].asText())
        assertEquals("2023-09-29", tilbakeIArbeid["undersporsmal"].first()["svar"].first()["verdi"].asText())
        val ferie = spørsmål.first { it["tag"].asText() == "FERIE_V2" }
        assertEquals("JA", ferie["svar"].first()["verdi"].asText())
        assertEquals("{\"fom\":\"2023-09-16\",\"tom\":\"2023-09-17\"}", ferie["undersporsmal"].first()["svar"].first()["verdi"].asText())
        val persmisjon = spørsmål.first { it["tag"].asText() == "PERMISJON_V2" }
        assertEquals("JA", persmisjon["svar"].first()["verdi"].asText())
        assertEquals("{\"fom\":\"2023-09-18\",\"tom\":\"2023-09-19\"}", persmisjon["undersporsmal"].first()["svar"].first()["verdi"].asText())
        val arbeidUnderveis = spørsmål.first { it["tag"].asText() == "ARBEID_UNDERVEIS_100_PROSENT_0" }
        assertEquals("JA", arbeidUnderveis["svar"].first()["verdi"].asText())
        val hvorMangeProsent = arbeidUnderveis["undersporsmal"].first { it["tag"].asText() == "HVOR_MYE_HAR_DU_JOBBET_0" }["undersporsmal"].first { it["tag"].asText() == "HVOR_MYE_PROSENT_0"}["undersporsmal"].first()
        assertEquals(20, hvorMangeProsent["svar"].first()["verdi"].asInt())
        val hvorMangeTimer = arbeidUnderveis["undersporsmal"].first { it["tag"].asText() == "HVOR_MYE_HAR_DU_JOBBET_0" }["undersporsmal"].none { it["tag"].asText() == "HVOR_MYE_TIMER_0"}
        assertTrue{hvorMangeTimer}
        val hvorMangeTimerVanligvis = arbeidUnderveis["undersporsmal"].first { it["tag"].asText() == "JOBBER_DU_NORMAL_ARBEIDSUKE_0" }
        assertEquals("NEI", hvorMangeTimerVanligvis["svar"].first()["verdi"].asText())
        assertEquals(40, hvorMangeTimerVanligvis["undersporsmal"].first()["svar"].first()["verdi"].asInt())
        val andreInntektskilder = spørsmål.first { it["tag"].asText() == "ANDRE_INNTEKTSKILDER"}
        assertEquals("NEI", andreInntektskilder["undersporsmal"].first()["undersporsmal"].first()["undersporsmal"].first()["svar"].first()["verdi"].asText())
        val kjenteInntektskilder = spørsmål.first { it["tag"].asText() == "KJENTE_INNTEKTSKILDER"}
        assertEquals("Jeg jobber turnus", kjenteInntektskilder["undersporsmal"].first()["undersporsmal"].first()["undersporsmal"].first()["undersporsmal"].first()["undersporsmal"].first()["undersporsmal"].first()["sporsmalstekst"].asText())
    }

    @Test
    fun `hentInntektsmelding query med riktige tilganger og paramtetre returnerer inntektsmelding`() {
        val dokumentId = UUID.randomUUID()

        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        every { dokumenthåndterer.håndter(any(), any(), any()) } returns objectMapper.readTree(
            inntektsmeldingJson()
        )
        val dokument = runQuery(
            """
            {
                hentInntektsmelding(
                    dokumentId: "$dokumentId"
                    fnr: "$FØDSELSNUMMER"
                ) {
                    arbeidsforholdId,
                    virksomhetsnummer,
                    begrunnelseForReduksjonEllerIkkeUtbetalt,
                    bruttoUtbetalt,
                    beregnetInntekt,
                    refusjon {
                        beloepPrMnd, opphoersdato
                    },
                    endringIRefusjoner { endringsdato, beloep },
                    opphoerAvNaturalytelser { naturalytelse, fom, beloepPrMnd },
                    gjenopptakelseNaturalytelser { naturalytelse, fom, beloepPrMnd },
                    arbeidsgiverperioder { fom, tom },
                    ferieperioder { fom, tom },
                    foersteFravaersdag,
                    naerRelasjon,
                    innsenderFulltNavn,
                    innsenderTelefon,
                    inntektEndringAarsak {
                        aarsak, perioder { 
                            fom, tom
                        }, gjelderFra, bleKjent                    
                    },
                    avsenderSystem {
                        navn,
                        versjon
                    }
                }
            }
        """
        )["data"]["hentInntektsmelding"]

        verify(exactly = 1) {
            dokumenthåndterer.håndter(
                fødselsnummer = FØDSELSNUMMER,
                dokumentId = dokumentId,
                dokumentType = DokumentType.INNTEKTSMELDING.name
            )
        }

        assertEquals(17, dokument.size())
        assertTrue(dokument["bruttoUtbetalt"].isNull)
        assertEquals(35000.0, dokument["beregnetInntekt"].asDouble())
        assertEquals("2023-08-01", dokument["foersteFravaersdag"].asText())
        assertEquals(0.0, dokument["refusjon"]["beloepPrMnd"].asDouble())
        assertTrue(dokument["refusjon"]["opphoersdato"].isNull)
        assertTrue(dokument["endringIRefusjoner"].isEmpty)
        assertTrue(dokument["opphoerAvNaturalytelser"].isEmpty)
        assertTrue(dokument["gjenopptakelseNaturalytelser"].isEmpty)
        assertEquals("2023-08-01", dokument["arbeidsgiverperioder"].first()["fom"].asText())
        assertEquals("2023-08-16", dokument["arbeidsgiverperioder"].first()["tom"].asText())
        assertTrue(dokument["ferieperioder"].isEmpty)
        assertEquals("2023-08-01", dokument["foersteFravaersdag"].asText())
        assertTrue(dokument["naerRelasjon"].isNull)
        assertEquals("MUSKULØS VALS", dokument["innsenderFulltNavn"].asText())
        assertEquals("12345678", dokument["innsenderTelefon"].asText())
        assertEquals("Tariffendring", dokument["inntektEndringAarsak"]["aarsak"].asText())
        assertTrue(dokument["inntektEndringAarsak"]["perioder"].isNull)
        assertEquals("2023-08-08", dokument["inntektEndringAarsak"]["gjelderFra"].asText())
        assertEquals("2023-09-12", dokument["inntektEndringAarsak"]["bleKjent"].asText())
    }

    @Language("JSON")
    private fun søknadJsonMedNeiSvar(arbeidGjenopptatt: String, sykmeldingSkrevet: String) = """{
  "type": "ARBEIDSTAKERE",
  "arbeidGjenopptatt": "$arbeidGjenopptatt",
  "sykmeldingSkrevet": "$sykmeldingSkrevet",
  "egenmeldingsdagerFraSykmelding": ["2018-01-01"],
  "soknadsperioder": [{"fom": "2018-01-01", "tom": "2018-01-31", "grad": 100, "faktiskGrad": null, "sykmeldingsgrad": 100}],
  "sporsmal": [
        {
          "id": "e82ceab9-f287-3642-8680-f59cd5c0ed28",
          "tag": "ANSVARSERKLARING",
          "sporsmalstekst": "Jeg vet at jeg kan miste retten til sykepenger hvis opplysningene jeg gir ikke er riktige eller fullstendige. Jeg vet også at NAV kan holde igjen eller kreve tilbake penger, og at å gi feil opplysninger kan være straffbart.",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "CHECKBOX_PANEL",
          "kriterieForVisningAvUndersporsmal": null,
          "svar": [
            {
              "verdi": "CHECKED"
            }
          ],
          "undersporsmal": []
        },
        {
          "id": "0d5e7763-6173-3030-85fa-b1dc868adf91",
          "tag": "TILBAKE_I_ARBEID",
          "sporsmalstekst": "Var du tilbake i fullt arbeid hos Sauefabrikk i løpet av perioden 1. - 31. juli 2023?",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "JA_NEI",
          "kriterieForVisningAvUndersporsmal": "JA",
          "svar": [
            {
              "verdi": "NEI"
            }
          ],
          "undersporsmal": [
            {
              "id": "1dca8597-f332-3135-940a-b8d91e5b4cfc",
              "tag": "TILBAKE_NAR",
              "sporsmalstekst": "Når begynte du å jobbe igjen?",
              "undertekst": null,
              "min": "2023-07-01",
              "max": "2023-07-31",
              "svartype": "DATO",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [],
              "undersporsmal": []
            }
          ]
        },
        {
          "id": "ece58998-574d-3ccf-ae68-275b893b9d57",
          "tag": "FERIE_V2",
          "sporsmalstekst": "Tok du ut feriedager i tidsrommet 1. - 31. juli 2023?",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "JA_NEI",
          "kriterieForVisningAvUndersporsmal": "JA",
          "svar": [
            {
              "verdi": "NEI"
            }
          ],
          "undersporsmal": [
            {
              "id": "83a5ba42-6a65-3465-892e-56e6040d9636",
              "tag": "FERIE_NAR_V2",
              "sporsmalstekst": "Når tok du ut feriedager?",
              "undertekst": null,
              "min": "2023-07-01",
              "max": "2023-07-31",
              "svartype": "PERIODER",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [],
              "undersporsmal": []
            }
          ]
        },
        {
          "id": "31072b0f-9a1b-3fd0-8ca7-48f8c8efdd5f",
          "tag": "PERMISJON_V2",
          "sporsmalstekst": "Tok du permisjon mens du var sykmeldt 1. - 31. juli 2023?",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "JA_NEI",
          "kriterieForVisningAvUndersporsmal": "JA",
          "svar": [
            {
              "verdi": "NEI"
            }
          ],
          "undersporsmal": [
            {
              "id": "796d9f27-3036-38bc-8c8f-e03dfa3fdd79",
              "tag": "PERMISJON_NAR_V2",
              "sporsmalstekst": "Når tok du permisjon?",
              "undertekst": null,
              "min": "2023-07-01",
              "max": "2023-07-31",
              "svartype": "PERIODER",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [],
              "undersporsmal": []
            }
          ]
        },
        {
          "id": "6f685ad5-022a-3503-b052-e59f20c9370c",
          "tag": "UTLAND_V2",
          "sporsmalstekst": "Var du på reise utenfor EØS mens du var sykmeldt 1. - 31. juli 2023?",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "JA_NEI",
          "kriterieForVisningAvUndersporsmal": "JA",
          "svar": [
            {
              "verdi": "NEI"
            }
          ],
          "undersporsmal": [
            {
              "id": "3f5aada9-0f22-3f99-b3c2-a35be07282a6",
              "tag": "UTLAND_NAR_V2",
              "sporsmalstekst": "Når var du utenfor EØS?",
              "undertekst": null,
              "min": "2023-07-01",
              "max": "2023-07-31",
              "svartype": "PERIODER",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [],
              "undersporsmal": []
            }
          ]
        },
        {
          "id": "f7fe6ff8-75c2-37ed-92d7-ed35bc7d089b",
          "tag": "ARBEID_UNDERVEIS_100_PROSENT_0",
          "sporsmalstekst": "I perioden 1. - 31. juli 2023 var du 100 % sykmeldt fra Sauefabrikk. Jobbet du noe hos Sauefabrikk i denne perioden?",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "JA_NEI",
          "kriterieForVisningAvUndersporsmal": "JA",
          "svar": [
            {
              "verdi": "NEI"
            }
          ],
          "undersporsmal": [
            {
              "id": "3edf3734-63a3-370d-ba88-9c656d1a1ea3",
              "tag": "HVOR_MYE_HAR_DU_JOBBET_0",
              "sporsmalstekst": "Oppgi arbeidsmengde i timer eller prosent:",
              "undertekst": null,
              "min": null,
              "max": null,
              "svartype": "RADIO_GRUPPE_TIMER_PROSENT",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [],
              "undersporsmal": [
                {
                  "id": "4723eef4-7836-34a9-8fd0-55c83da7fadf",
                  "tag": "HVOR_MYE_PROSENT_0",
                  "sporsmalstekst": "Prosent",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "RADIO",
                  "kriterieForVisningAvUndersporsmal": "CHECKED",
                  "svar": [],
                  "undersporsmal": [
                    {
                      "id": "4fd704fb-978e-37f6-8d83-13291de1b8e4",
                      "tag": "HVOR_MYE_PROSENT_VERDI_0",
                      "sporsmalstekst": "Oppgi hvor mange prosent av din normale arbeidstid du jobbet hos Sauefabrikk i perioden 1. - 31. juli 2023?",
                      "undertekst": "Oppgi i prosent. Eksempel: 40",
                      "min": "1",
                      "max": "99",
                      "svartype": "PROSENT",
                      "kriterieForVisningAvUndersporsmal": null,
                      "svar": [],
                      "undersporsmal": []
                    }
                  ]
                },
                {
                  "id": "21acb29a-3281-303e-8f82-90b8fa7dc9a7",
                  "tag": "HVOR_MYE_TIMER_0",
                  "sporsmalstekst": "Timer",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "RADIO",
                  "kriterieForVisningAvUndersporsmal": "CHECKED",
                  "svar": [],
                  "undersporsmal": [
                    {
                      "id": "b1987d25-cec2-3557-994f-8312fcefe346",
                      "tag": "HVOR_MYE_TIMER_VERDI_0",
                      "sporsmalstekst": "Oppgi totalt antall timer du jobbet i perioden 1. - 31. juli 2023 hos Sauefabrikk",
                      "undertekst": "Oppgi i timer. Eksempel: 12",
                      "min": "1",
                      "max": "664",
                      "svartype": "TIMER",
                      "kriterieForVisningAvUndersporsmal": null,
                      "svar": [],
                      "undersporsmal": []
                    }
                  ]
                }
              ]
            },
            {
              "id": "3b08ef95-c1cd-3412-b676-2fe7254e9488",
              "tag": "JOBBER_DU_NORMAL_ARBEIDSUKE_0",
              "sporsmalstekst": "Jobber du vanligvis 37,5 timer i uka hos Sauefabrikk?",
              "undertekst": null,
              "min": null,
              "max": null,
              "svartype": "JA_NEI",
              "kriterieForVisningAvUndersporsmal": "NEI",
              "svar": [],
              "undersporsmal": [
                {
                  "id": "09a3b551-7107-3c96-87a3-b0c64b5b2219",
                  "tag": "HVOR_MANGE_TIMER_PER_UKE_0",
                  "sporsmalstekst": "Oppgi timer per uke",
                  "undertekst": null,
                  "min": "1",
                  "max": "150",
                  "svartype": "TIMER",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                }
              ]
            }
          ]
        },
        {
          "id": "a6cd0e23-dfc6-375b-b1bc-42eb4221afd2",
          "tag": "ARBEID_UTENFOR_NORGE",
          "sporsmalstekst": "Har du arbeidet i utlandet i løpet av de siste 12 månedene?",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "JA_NEI",
          "kriterieForVisningAvUndersporsmal": null,
          "svar": [
            {
              "verdi": "NEI"
            }
          ],
          "undersporsmal": []
        },
        {
          "id": "265c547c-7bc4-3540-92a3-ff3b35c25afd",
          "tag": "ANDRE_INNTEKTSKILDER_V2",
          "sporsmalstekst": "Har du andre inntektskilder enn Sauefabrikk?",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "JA_NEI",
          "kriterieForVisningAvUndersporsmal": "JA",
          "svar": [
            {
              "verdi": "NEI"
            }
          ],
          "undersporsmal": [
            {
              "id": "6d0b3ce9-5eaa-34a9-9201-6aeadfb6d813",
              "tag": "HVILKE_ANDRE_INNTEKTSKILDER",
              "sporsmalstekst": "Velg inntektskildene som passer for deg:",
              "undertekst": "Finner du ikke noe som passer for deg, svarer du nei på spørsmålet over",
              "min": null,
              "max": null,
              "svartype": "CHECKBOX_GRUPPE",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [],
              "undersporsmal": [
                {
                  "id": "b1f45608-6fdd-3f6b-b42f-ca66844e1ed5",
                  "tag": "INNTEKTSKILDE_ANDRE_ARBEIDSFORHOLD",
                  "sporsmalstekst": "Ansatt andre steder enn nevnt over",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "CHECKBOX",
                  "kriterieForVisningAvUndersporsmal": "CHECKED",
                  "svar": [],
                  "undersporsmal": [
                    {
                      "id": "a57cf0f6-79ce-3f9c-b3a3-b421ddb0afa9",
                      "tag": "INNTEKTSKILDE_ANDRE_ARBEIDSFORHOLD_JOBBET_I_DET_SISTE",
                      "sporsmalstekst": "Har du jobbet for eller mottatt inntekt fra én eller flere av disse arbeidsgiverne de siste 14 dagene før du ble sykmeldt?",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "JA_NEI",
                      "kriterieForVisningAvUndersporsmal": null,
                      "svar": [],
                      "undersporsmal": []
                    }
                  ]
                },
                {
                  "id": "3b946726-9818-3b46-ba16-63c7f6be768d",
                  "tag": "INNTEKTSKILDE_SELVSTENDIG",
                  "sporsmalstekst": "Selvstendig næringsdrivende",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "CHECKBOX",
                  "kriterieForVisningAvUndersporsmal": "CHECKED",
                  "svar": [],
                  "undersporsmal": [
                    {
                      "id": "3238e923-5cc1-3e7c-822f-0b4a6c753438",
                      "tag": "INNTEKTSKILDE_SELVSTENDIG_N_AR",
                      "sporsmalstekst": "Har du vært næringsdrivende i mer enn 5 år?",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "JA_NEI",
                      "kriterieForVisningAvUndersporsmal": "JA",
                      "svar": [],
                      "undersporsmal": [
                        {
                          "id": "0d4273ad-129a-3724-b7ba-0a67c724362e",
                          "tag": "INNTEKTSKILDE_SELVSTENDIG_VARIG_ENDRING_GRUPPE",
                          "sporsmalstekst": "Har det vært endring i din arbeidssituasjon eller virksomhet?",
                          "undertekst": null,
                          "min": null,
                          "max": null,
                          "svartype": "RADIO_GRUPPE",
                          "kriterieForVisningAvUndersporsmal": null,
                          "svar": [],
                          "undersporsmal": [
                            {
                              "id": "b239559b-a394-3703-8c15-9b804435eef3",
                              "tag": "INNTEKTSKILDE_SELVSTENDIG_VARIG_ENDRING_JA",
                              "sporsmalstekst": "Ja",
                              "undertekst": null,
                              "min": null,
                              "max": null,
                              "svartype": "RADIO",
                              "kriterieForVisningAvUndersporsmal": null,
                              "svar": [],
                              "undersporsmal": []
                            },
                            {
                              "id": "f6848ffd-0903-3f26-897c-fb6a537e9001",
                              "tag": "INNTEKTSKILDE_SELVSTENDIG_VARIG_ENDRING_NEI",
                              "sporsmalstekst": "Nei",
                              "undertekst": null,
                              "min": null,
                              "max": null,
                              "svartype": "RADIO",
                              "kriterieForVisningAvUndersporsmal": null,
                              "svar": [],
                              "undersporsmal": []
                            },
                            {
                              "id": "6c419160-4067-37a9-9087-8b43d9a54da5",
                              "tag": "INNTEKTSKILDE_SELVSTENDIG_VARIG_ENDRING_VET_IKKE",
                              "sporsmalstekst": "Vet ikke",
                              "undertekst": null,
                              "min": null,
                              "max": null,
                              "svartype": "RADIO",
                              "kriterieForVisningAvUndersporsmal": null,
                              "svar": [],
                              "undersporsmal": []
                            }
                          ]
                        }
                      ]
                    }
                  ]
                },
                {
                  "id": "c8017056-7267-3bc7-aefa-3cca882dae92",
                  "tag": "INNTEKTSKILDE_SELVSTENDIG_DAGMAMMA",
                  "sporsmalstekst": "Dagmamma",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "CHECKBOX",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                },
                {
                  "id": "3d583e10-8808-304b-83cd-2ef076b986eb",
                  "tag": "INNTEKTSKILDE_JORDBRUKER",
                  "sporsmalstekst": "Jordbruk / Fiske / Reindrift",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "CHECKBOX",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                },
                {
                  "id": "97546779-08ce-3f02-a89b-55b160b55c58",
                  "tag": "INNTEKTSKILDE_FRILANSER",
                  "sporsmalstekst": "Frilanser",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "CHECKBOX",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                },
                {
                  "id": "04f22f81-92f1-3063-ba4f-ff71ef4c302b",
                  "tag": "INNTEKTSKILDE_OMSORGSLONN",
                  "sporsmalstekst": "Kommunal omsorgstønad",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "CHECKBOX",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                },
                {
                  "id": "c5a3db37-c36d-369b-a4a3-468920e70400",
                  "tag": "INNTEKTSKILDE_FOSTERHJEM",
                  "sporsmalstekst": "Fosterhjemsgodtgjørelse",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "CHECKBOX",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                },
                {
                  "id": "6c00960c-5b32-326c-9c33-e135d02da524",
                  "tag": "INNTEKTSKILDE_STYREVERV",
                  "sporsmalstekst": "Styreverv",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "CHECKBOX",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                }
              ]
            }
          ]
        },
        {
          "id": "ce37719d-0cfd-3f0b-a1ee-907608eb02be",
          "tag": "VAER_KLAR_OVER_AT",
          "sporsmalstekst": "Viktig å være klar over:",
          "undertekst": "<ul><li>Du kan bare få sykepenger hvis det er din egen sykdom eller skade som hindrer deg i å jobbe. Sosiale eller økonomiske problemer gir ikke rett til sykepenger.</li><li>Du kan miste retten til sykepenger hvis du nekter å opplyse om din egen arbeidsevne, eller hvis du ikke tar imot behandling eller tilrettelegging.</li><li>Retten til sykepenger gjelder bare inntekt du har mottatt som lønn og betalt skatt av på sykmeldingstidspunktet.</li><li>NAV kan innhente opplysninger som er nødvendige for å behandle søknaden.</li><li>Fristen for å søke sykepenger er som hovedregel 3 måneder.</li><li>Du kan endre svarene i denne søknaden opp til 12 måneder etter du sendte den inn første gangen.</li><li>Du må melde fra til NAV hvis du satt i varetekt, sonet straff eller var under forvaring i sykmeldingsperioden. <a href=\"https://www.nav.no/skriv-til-oss\" target=\"_blank\">Meld fra til NAV her.</a></li><li>Du må melde fra om studier som er påbegynt etter at du ble sykmeldt, og som ikke er avklart med NAV. Det samme gjelder hvis du begynner å studere mer enn du gjorde før du ble sykmeldt. <a href=\"https://www.nav.no/skriv-til-oss\" target=\"_blank\">Meld fra til NAV her.</a></li><li>Du kan lese mer om rettigheter og plikter på <a href=\"https://www.nav.no/sykepenger\" target=\"_blank\">nav.no/sykepenger</a>.</li></ul>",
          "min": null,
          "max": null,
          "svartype": "IKKE_RELEVANT",
          "kriterieForVisningAvUndersporsmal": null,
          "svar": [],
          "undersporsmal": []
        },
        {
          "id": "f033e620-e5f3-31a7-b76a-8cd0c8f0a941",
          "tag": "BEKREFT_OPPLYSNINGER",
          "sporsmalstekst": "Jeg har lest all informasjonen jeg har fått i søknaden og bekrefter at opplysningene jeg har gitt er korrekte.",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "CHECKBOX_PANEL",
          "kriterieForVisningAvUndersporsmal": null,
          "svar": [
            {
              "verdi": "CHECKED"
            }
          ],
          "undersporsmal": []
        }
      ]
}
""".trimIndent()

    @Language("JSON")
    private fun søknadJsonMedJaSvar() = """{
  "type": "ARBEIDSTAKERE",
  "arbeidGjenopptatt": "${LocalDate.now()}",
  "sykmeldingSkrevet": "${LocalDateTime.now()}",
  "egenmeldingsdagerFraSykmelding": ["2018-01-01"],
  "soknadsperioder": [{"fom": "2018-01-01", "tom": "2018-01-31", "grad": 100, "faktiskGrad": null, "sykmeldingsgrad":  100}],
  "sporsmal": [
        {
          "id": "6871a262-214c-322f-b317-0b6feb1fcf06",
          "tag": "ANSVARSERKLARING",
          "sporsmalstekst": "Jeg vet at jeg kan miste retten til sykepenger hvis opplysningene jeg gir ikke er riktige eller fullstendige. Jeg vet også at NAV kan holde igjen eller kreve tilbake penger, og at å gi feil opplysninger kan være straffbart.",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "CHECKBOX_PANEL",
          "kriterieForVisningAvUndersporsmal": null,
          "svar": [
            {
              "verdi": "CHECKED"
            }
          ],
          "undersporsmal": []
        },
        {
          "id": "e29c6c88-ff0a-3948-8e07-4577ae1bb4bb",
          "tag": "TILBAKE_I_ARBEID",
          "sporsmalstekst": "Var du tilbake i fullt arbeid hos Pengeløs Sparebank i løpet av perioden 1. - 30. september 2023?",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "JA_NEI",
          "kriterieForVisningAvUndersporsmal": "JA",
          "svar": [
            {
              "verdi": "JA"
            }
          ],
          "undersporsmal": [
            {
              "id": "4cd65416-4605-3315-a723-0a4a75bb2cdb",
              "tag": "TILBAKE_NAR",
              "sporsmalstekst": "Når begynte du å jobbe igjen?",
              "undertekst": null,
              "min": "2023-09-01",
              "max": "2023-09-30",
              "svartype": "DATO",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [
                {
                  "verdi": "2023-09-29"
                }
              ],
              "undersporsmal": []
            }
          ]
        },
        {
          "id": "5d863cbf-5218-3117-9083-701ea07595e7",
          "tag": "FERIE_V2",
          "sporsmalstekst": "Tok du ut feriedager i tidsrommet 1. - 28. september 2023?",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "JA_NEI",
          "kriterieForVisningAvUndersporsmal": "JA",
          "svar": [
            {
              "verdi": "JA"
            }
          ],
          "undersporsmal": [
            {
              "id": "f5798725-475c-3b93-b90a-6fe4e34d9418",
              "tag": "FERIE_NAR_V2",
              "sporsmalstekst": "Når tok du ut feriedager?",
              "undertekst": null,
              "min": "2023-09-01",
              "max": "2023-09-28",
              "svartype": "PERIODER",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [
                {
                  "verdi": "{\"fom\":\"2023-09-16\",\"tom\":\"2023-09-17\"}"
                }
              ],
              "undersporsmal": []
            }
          ]
        },
        {
          "id": "f3e93b91-a158-3cd9-814b-dd91144f9153",
          "tag": "PERMISJON_V2",
          "sporsmalstekst": "Tok du permisjon mens du var sykmeldt 1. - 28. september 2023?",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "JA_NEI",
          "kriterieForVisningAvUndersporsmal": "JA",
          "svar": [
            {
              "verdi": "JA"
            }
          ],
          "undersporsmal": [
            {
              "id": "7ec31638-69af-35db-9d4d-2583e9a25c70",
              "tag": "PERMISJON_NAR_V2",
              "sporsmalstekst": "Når tok du permisjon?",
              "undertekst": null,
              "min": "2023-09-01",
              "max": "2023-09-28",
              "svartype": "PERIODER",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [
                {
                  "verdi": "{\"fom\":\"2023-09-18\",\"tom\":\"2023-09-19\"}"
                }
              ],
              "undersporsmal": []
            }
          ]
        },
        {
          "id": "5286d2d7-7df4-39e2-a4b2-8648a66f0930",
          "tag": "UTLAND_V2",
          "sporsmalstekst": "Var du på reise utenfor EØS mens du var sykmeldt 1. - 28. september 2023?",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "JA_NEI",
          "kriterieForVisningAvUndersporsmal": "JA",
          "svar": [
            {
              "verdi": "NEI"
            }
          ],
          "undersporsmal": [
            {
              "id": "0bebed8c-6c8c-3145-9390-a9b1cf76474b",
              "tag": "UTLAND_NAR_V2",
              "sporsmalstekst": "Når var du utenfor EØS?",
              "undertekst": null,
              "min": "2023-09-01",
              "max": "2023-09-28",
              "svartype": "PERIODER",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [],
              "undersporsmal": []
            }
          ]
        },
        {
          "id": "a12994ff-3f02-34c2-a699-2a7933c61912",
          "tag": "ARBEID_UNDERVEIS_100_PROSENT_0",
          "sporsmalstekst": "I perioden 1. - 28. september 2023 var du 100 % sykmeldt fra Pengeløs Sparebank. Jobbet du noe hos Pengeløs Sparebank i denne perioden?",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "JA_NEI",
          "kriterieForVisningAvUndersporsmal": "JA",
          "svar": [
            {
              "verdi": "JA"
            }
          ],
          "undersporsmal": [
            {
              "id": "4fdb9a26-cbf6-30bd-80e7-e985738efb7a",
              "tag": "HVOR_MYE_HAR_DU_JOBBET_0",
              "sporsmalstekst": "Oppgi arbeidsmengde i timer eller prosent:",
              "undertekst": null,
              "min": null,
              "max": null,
              "svartype": "RADIO_GRUPPE_TIMER_PROSENT",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [],
              "undersporsmal": [
                {
                  "id": "5b00023f-b11d-3a07-9614-84829ef1de96",
                  "tag": "HVOR_MYE_PROSENT_0",
                  "sporsmalstekst": "Prosent",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "RADIO",
                  "kriterieForVisningAvUndersporsmal": "CHECKED",
                  "svar": [
                    {
                      "verdi": "CHECKED"
                    }
                  ],
                  "undersporsmal": [
                    {
                      "id": "d58bba81-3386-3c32-896b-f3fea0c8116d",
                      "tag": "HVOR_MYE_PROSENT_VERDI_0",
                      "sporsmalstekst": "Oppgi hvor mange prosent av din normale arbeidstid du jobbet hos Pengeløs Sparebank i perioden 1. - 28. september 2023?",
                      "undertekst": "Oppgi i prosent. Eksempel: 40",
                      "min": "1",
                      "max": "99",
                      "svartype": "PROSENT",
                      "kriterieForVisningAvUndersporsmal": null,
                      "svar": [
                        {
                          "verdi": "20"
                        }
                      ],
                      "undersporsmal": []
                    }
                  ]
                },
                {
                  "id": "22b758a1-b9a8-3d1d-aa0a-eb9515df8df6",
                  "tag": "HVOR_MYE_TIMER_0",
                  "sporsmalstekst": "Timer",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "RADIO",
                  "kriterieForVisningAvUndersporsmal": "CHECKED",
                  "svar": [],
                  "undersporsmal": [
                    {
                      "id": "7a34748d-7b2b-3f0c-a086-2093f8d9a7dd",
                      "tag": "HVOR_MYE_TIMER_VERDI_0",
                      "sporsmalstekst": "Oppgi totalt antall timer du jobbet i perioden 1. - 28. september 2023 hos Pengeløs Sparebank",
                      "undertekst": "Oppgi i timer. Eksempel: 12",
                      "min": "1",
                      "max": "600",
                      "svartype": "TIMER",
                      "kriterieForVisningAvUndersporsmal": null,
                      "svar": [],
                      "undersporsmal": []
                    }
                  ]
                }
              ]
            },
            {
              "id": "82d13447-f564-31a9-8d60-aaf9de04facb",
              "tag": "JOBBER_DU_NORMAL_ARBEIDSUKE_0",
              "sporsmalstekst": "Jobber du vanligvis 37,5 timer i uka hos Pengeløs Sparebank?",
              "undertekst": null,
              "min": null,
              "max": null,
              "svartype": "JA_NEI",
              "kriterieForVisningAvUndersporsmal": "NEI",
              "svar": [
                {
                  "verdi": "NEI"
                }
              ],
              "undersporsmal": [
                {
                  "id": "7547444f-3a9a-3985-a909-3f3a1547928d",
                  "tag": "HVOR_MANGE_TIMER_PER_UKE_0",
                  "sporsmalstekst": "Oppgi timer per uke",
                  "undertekst": null,
                  "min": "1",
                  "max": "150",
                  "svartype": "TIMER",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [
                    {
                      "verdi": "40"
                    }
                  ],
                  "undersporsmal": []
                }
              ]
            }
          ]
        },
        {
          "id": "9018f180-44a8-3757-8df8-4fa76882f746",
          "tag": "KJENTE_INNTEKTSKILDER",
          "sporsmalstekst": "Du er oppført med flere inntektskilder i Arbeidsgiver- og arbeidstakerregisteret. Vi trenger mer informasjon om disse.",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "GRUPPE_AV_UNDERSPORSMAL",
          "kriterieForVisningAvUndersporsmal": null,
          "svar": [],
          "undersporsmal": [
            {
              "id": "c397ce1f-77f3-3576-966c-a5ea2c421c0e",
              "tag": "KJENTE_INNTEKTSKILDER_GRUPPE_0",
              "sporsmalstekst": null,
              "undertekst": null,
              "min": null,
              "max": null,
              "svartype": "GRUPPE_AV_UNDERSPORSMAL",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [],
              "undersporsmal": [
                {
                  "id": "857d8ff6-b4bb-3b0d-8b77-d2b3b42e8cea",
                  "tag": "KJENTE_INNTEKTSKILDER_GRUPPE_TITTEL_0",
                  "sporsmalstekst": "Sjokkerende Elektriker",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "IKKE_RELEVANT",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                },
                {
                  "id": "5b8da937-4beb-3608-a7f8-9f1371d083f2",
                  "tag": "KJENTE_INNTEKTSKILDER_SLUTTET_0",
                  "sporsmalstekst": "Har du sluttet hos Sjokkerende Elektriker før du ble sykmeldt 13. november 2023?",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "RADIO_GRUPPE",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": [
                    {
                      "id": "486be174-9d07-3404-91e7-8572f1f263a6",
                      "tag": "KJENTE_INNTEKTSKILDER_SLUTTET_JA_0",
                      "sporsmalstekst": "Ja",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "RADIO",
                      "kriterieForVisningAvUndersporsmal": "CHECKED",
                      "svar": [],
                      "undersporsmal": [
                        {
                          "id": "6fe4df44-c000-3cb9-9a50-327a0955d3cf",
                          "tag": "KJENTE_INNTEKTSKILDER_DATO_SLUTTET_0",
                          "sporsmalstekst": "Når sluttet du?",
                          "undertekst": null,
                          "min": null,
                          "max": "2023-11-12",
                          "svartype": "DATO",
                          "kriterieForVisningAvUndersporsmal": null,
                          "svar": [],
                          "undersporsmal": []
                        }
                      ]
                    },
                    {
                      "id": "dd1a2c49-8109-3015-b80d-8a9edb89fe30",
                      "tag": "KJENTE_INNTEKTSKILDER_SLUTTET_NEI_0",
                      "sporsmalstekst": "Nei",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "RADIO",
                      "kriterieForVisningAvUndersporsmal": "CHECKED",
                      "svar": [
                        {
                          "verdi": "CHECKED"
                        }
                      ],
                      "undersporsmal": [
                        {
                          "id": "74d7d49e-1d57-33b5-85e6-6da256e10736",
                          "tag": "KJENTE_INNTEKTSKILDER_UTFORT_ARBEID_0",
                          "sporsmalstekst": "Har du utført noe arbeid ved Sjokkerende Elektriker i perioden 29. oktober - 12. november 2023?",
                          "undertekst": null,
                          "min": null,
                          "max": null,
                          "svartype": "JA_NEI",
                          "kriterieForVisningAvUndersporsmal": "NEI",
                          "svar": [
                            {
                              "verdi": "NEI"
                            }
                          ],
                          "undersporsmal": [
                            {
                              "id": "8e99c11d-21dd-3d27-aa35-a771e147cb85",
                              "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_0",
                              "sporsmalstekst": "Velg en eller flere årsaker til at du ikke har jobbet",
                              "undertekst": null,
                              "min": null,
                              "max": null,
                              "svartype": "CHECKBOX_GRUPPE",
                              "kriterieForVisningAvUndersporsmal": null,
                              "svar": [],
                              "undersporsmal": [
                                {
                                  "id": "3ec5dff7-2c1e-34d8-85a6-6f514a03e6d7",
                                  "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_SYKMELDT_0",
                                  "sporsmalstekst": "Jeg var sykmeldt",
                                  "undertekst": null,
                                  "min": null,
                                  "max": null,
                                  "svartype": "CHECKBOX",
                                  "kriterieForVisningAvUndersporsmal": null,
                                  "svar": [],
                                  "undersporsmal": []
                                },
                                {
                                  "id": "1a506231-2da3-3b85-84c3-9ed38b7ebb23",
                                  "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_TURNUS_0",
                                  "sporsmalstekst": "Jeg jobber turnus",
                                  "undertekst": null,
                                  "min": null,
                                  "max": null,
                                  "svartype": "CHECKBOX",
                                  "kriterieForVisningAvUndersporsmal": null,
                                  "svar": [
                                    {
                                      "verdi": "CHECKED"
                                    }
                                  ],
                                  "undersporsmal": []
                                },
                                {
                                  "id": "6070aadd-47b3-321c-8b58-c9b304428840",
                                  "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_FERIE_0",
                                  "sporsmalstekst": "Jeg hadde lovbestemt ferie",
                                  "undertekst": null,
                                  "min": null,
                                  "max": null,
                                  "svartype": "CHECKBOX",
                                  "kriterieForVisningAvUndersporsmal": null,
                                  "svar": [],
                                  "undersporsmal": []
                                },
                                {
                                  "id": "b41df6dd-826e-301d-9bb2-792087aa0221",
                                  "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_AVSPASERING_0",
                                  "sporsmalstekst": "Jeg avspaserte",
                                  "undertekst": null,
                                  "min": null,
                                  "max": null,
                                  "svartype": "CHECKBOX",
                                  "kriterieForVisningAvUndersporsmal": null,
                                  "svar": [],
                                  "undersporsmal": []
                                },
                                {
                                  "id": "1d27b566-27ab-3c66-b18f-5e4446ebbedc",
                                  "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_PERMITTERT_0",
                                  "sporsmalstekst": "Jeg var permittert",
                                  "undertekst": null,
                                  "min": null,
                                  "max": null,
                                  "svartype": "CHECKBOX",
                                  "kriterieForVisningAvUndersporsmal": null,
                                  "svar": [],
                                  "undersporsmal": []
                                },
                                {
                                  "id": "02f821a2-7a26-3f0b-a5e7-a331b23421b9",
                                  "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_PERMISJON_0",
                                  "sporsmalstekst": "Jeg hadde permisjon",
                                  "undertekst": null,
                                  "min": null,
                                  "max": null,
                                  "svartype": "CHECKBOX",
                                  "kriterieForVisningAvUndersporsmal": null,
                                  "svar": [],
                                  "undersporsmal": []
                                },
                                {
                                  "id": "a43da7cf-6bd3-3262-a88f-99a7c8a2072b",
                                  "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_ANNEN_0",
                                  "sporsmalstekst": "Annen årsak",
                                  "undertekst": null,
                                  "min": null,
                                  "max": null,
                                  "svartype": "CHECKBOX",
                                  "kriterieForVisningAvUndersporsmal": null,
                                  "svar": [],
                                  "undersporsmal": []
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            },
            {
              "id": "028dab02-e655-3b5d-a7b7-f67968393407",
              "tag": "KJENTE_INNTEKTSKILDER_GRUPPE_1",
              "sporsmalstekst": null,
              "undertekst": null,
              "min": null,
              "max": null,
              "svartype": "GRUPPE_AV_UNDERSPORSMAL",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [],
              "undersporsmal": [
                {
                  "id": "08dcff68-f7e0-328c-81a7-660534321837",
                  "tag": "KJENTE_INNTEKTSKILDER_GRUPPE_TITTEL_1",
                  "sporsmalstekst": "Pengeløs Sparebank",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "IKKE_RELEVANT",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                },
                {
                  "id": "7ab795e8-55a5-3057-b03b-7c6d27cde342",
                  "tag": "KJENTE_INNTEKTSKILDER_SLUTTET_1",
                  "sporsmalstekst": "Har du sluttet hos Pengeløs Sparebank før du ble sykmeldt 13. november 2023?",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "RADIO_GRUPPE",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": [
                    {
                      "id": "488da78d-2a92-30c9-8d66-2c2149587241",
                      "tag": "KJENTE_INNTEKTSKILDER_SLUTTET_JA_1",
                      "sporsmalstekst": "Ja",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "RADIO",
                      "kriterieForVisningAvUndersporsmal": "CHECKED",
                      "svar": [
                        {
                          "verdi": "CHECKED"
                        }
                      ],
                      "undersporsmal": [
                        {
                          "id": "0a28d4a3-01b6-3ece-a85e-bcdccfed58cb",
                          "tag": "KJENTE_INNTEKTSKILDER_DATO_SLUTTET_1",
                          "sporsmalstekst": "Når sluttet du?",
                          "undertekst": null,
                          "min": null,
                          "max": "2023-11-12",
                          "svartype": "DATO",
                          "kriterieForVisningAvUndersporsmal": null,
                          "svar": [
                            {
                              "verdi": "2023-11-01"
                            }
                          ],
                          "undersporsmal": []
                        }
                      ]
                    },
                    {
                      "id": "63de7596-e0c7-3a1b-a709-b40217c9553c",
                      "tag": "KJENTE_INNTEKTSKILDER_SLUTTET_NEI_1",
                      "sporsmalstekst": "Nei",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "RADIO",
                      "kriterieForVisningAvUndersporsmal": "CHECKED",
                      "svar": [],
                      "undersporsmal": [
                        {
                          "id": "8e0d6542-8fe5-3d5d-b01d-983c68e07a43",
                          "tag": "KJENTE_INNTEKTSKILDER_UTFORT_ARBEID_1",
                          "sporsmalstekst": "Har du utført noe arbeid ved Pengeløs Sparebank i perioden 29. oktober - 12. november 2023?",
                          "undertekst": null,
                          "min": null,
                          "max": null,
                          "svartype": "JA_NEI",
                          "kriterieForVisningAvUndersporsmal": "NEI",
                          "svar": [],
                          "undersporsmal": [
                            {
                              "id": "e2525bce-1843-3440-8ca8-8299291a1367",
                              "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_1",
                              "sporsmalstekst": "Velg en eller flere årsaker til at du ikke har jobbet",
                              "undertekst": null,
                              "min": null,
                              "max": null,
                              "svartype": "CHECKBOX_GRUPPE",
                              "kriterieForVisningAvUndersporsmal": null,
                              "svar": [],
                              "undersporsmal": [
                                {
                                  "id": "3a04d4e5-a107-3a32-a5b4-7da211f01953",
                                  "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_SYKMELDT_1",
                                  "sporsmalstekst": "Jeg var sykmeldt",
                                  "undertekst": null,
                                  "min": null,
                                  "max": null,
                                  "svartype": "CHECKBOX",
                                  "kriterieForVisningAvUndersporsmal": null,
                                  "svar": [],
                                  "undersporsmal": []
                                },
                                {
                                  "id": "41c9b471-0d06-3211-970a-adbed4b57531",
                                  "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_TURNUS_1",
                                  "sporsmalstekst": "Jeg jobber turnus",
                                  "undertekst": null,
                                  "min": null,
                                  "max": null,
                                  "svartype": "CHECKBOX",
                                  "kriterieForVisningAvUndersporsmal": null,
                                  "svar": [],
                                  "undersporsmal": []
                                },
                                {
                                  "id": "36cd0e00-0c07-3e87-9231-9fbad5c194ca",
                                  "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_FERIE_1",
                                  "sporsmalstekst": "Jeg hadde lovbestemt ferie",
                                  "undertekst": null,
                                  "min": null,
                                  "max": null,
                                  "svartype": "CHECKBOX",
                                  "kriterieForVisningAvUndersporsmal": null,
                                  "svar": [],
                                  "undersporsmal": []
                                },
                                {
                                  "id": "5777bf40-62d0-353d-aa29-704df2ccc36e",
                                  "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_AVSPASERING_1",
                                  "sporsmalstekst": "Jeg avspaserte",
                                  "undertekst": null,
                                  "min": null,
                                  "max": null,
                                  "svartype": "CHECKBOX",
                                  "kriterieForVisningAvUndersporsmal": null,
                                  "svar": [],
                                  "undersporsmal": []
                                },
                                {
                                  "id": "2988b4d6-6471-3a62-9edb-4831026da26b",
                                  "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_PERMITTERT_1",
                                  "sporsmalstekst": "Jeg var permittert",
                                  "undertekst": null,
                                  "min": null,
                                  "max": null,
                                  "svartype": "CHECKBOX",
                                  "kriterieForVisningAvUndersporsmal": null,
                                  "svar": [],
                                  "undersporsmal": []
                                },
                                {
                                  "id": "561a1ff8-b9fa-3933-a528-4e5305c69d91",
                                  "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_PERMISJON_1",
                                  "sporsmalstekst": "Jeg hadde permisjon",
                                  "undertekst": null,
                                  "min": null,
                                  "max": null,
                                  "svartype": "CHECKBOX",
                                  "kriterieForVisningAvUndersporsmal": null,
                                  "svar": [],
                                  "undersporsmal": []
                                },
                                {
                                  "id": "a2af9109-b73f-366c-a29a-0ce5301e67e1",
                                  "tag": "KJENTE_INNTEKTSKILDER_ARSAK_IKKE_JOBBET_ANNEN_1",
                                  "sporsmalstekst": "Annen årsak",
                                  "undertekst": null,
                                  "min": null,
                                  "max": null,
                                  "svartype": "CHECKBOX",
                                  "kriterieForVisningAvUndersporsmal": null,
                                  "svar": [],
                                  "undersporsmal": []
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          "id": "4da1a113-f636-354b-a825-df2de348f621",
          "tag": "ANDRE_INNTEKTSKILDER",
          "sporsmalstekst": "Har du andre inntektskilder enn TROMØY BOKOLLEKTIV?",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "JA_NEI",
          "kriterieForVisningAvUndersporsmal": "JA",
          "svar": [
            {
              "verdi": "JA"
            }
          ],
          "undersporsmal": [
            {
              "id": "bc8e81df-754c-38a1-a618-3b006cd3e41e",
              "tag": "HVILKE_ANDRE_INNTEKTSKILDER",
              "sporsmalstekst": "Hvilke andre inntektskilder har du?",
              "undertekst": null,
              "min": null,
              "max": null,
              "svartype": "CHECKBOX_GRUPPE",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [],
              "undersporsmal": [
                {
                  "id": "b7cb9ec2-8fc2-3cfc-9b85-68b89f187e98",
                  "tag": "INNTEKTSKILDE_ANDRE_ARBEIDSFORHOLD",
                  "sporsmalstekst": "andre arbeidsforhold",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "CHECKBOX",
                  "kriterieForVisningAvUndersporsmal": "CHECKED",
                  "svar": [
                    {
                      "verdi": "CHECKED"
                    }
                  ],
                  "undersporsmal": [
                    {
                      "id": "a8036689-091b-3772-94e9-06049cfa3fab",
                      "tag": "INNTEKTSKILDE_ANDRE_ARBEIDSFORHOLD_ER_DU_SYKMELDT",
                      "sporsmalstekst": "Er du sykmeldt fra dette?",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "JA_NEI",
                      "kriterieForVisningAvUndersporsmal": null,
                      "svar": [
                        {
                          "verdi": "NEI"
                        }
                      ],
                      "undersporsmal": []
                    }
                  ]
                },
                {
                  "id": "5d337b0e-3b7e-30bf-af51-fdc26864863f",
                  "tag": "INNTEKTSKILDE_SELVSTENDIG",
                  "sporsmalstekst": "selvstendig næringsdrivende",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "CHECKBOX",
                  "kriterieForVisningAvUndersporsmal": "CHECKED",
                  "svar": [],
                  "undersporsmal": [
                    {
                      "id": "d6ce6d10-b597-381e-8c26-eac2d5238dfd",
                      "tag": "INNTEKTSKILDE_SELVSTENDIG_ER_DU_SYKMELDT",
                      "sporsmalstekst": "Er du sykmeldt fra dette?",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "JA_NEI",
                      "kriterieForVisningAvUndersporsmal": null,
                      "svar": [],
                      "undersporsmal": []
                    }
                  ]
                },
                {
                  "id": "39eb093f-8bb3-3ba9-a997-4fe3f08afff0",
                  "tag": "INNTEKTSKILDE_SELVSTENDIG_DAGMAMMA",
                  "sporsmalstekst": "dagmamma",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "CHECKBOX",
                  "kriterieForVisningAvUndersporsmal": "CHECKED",
                  "svar": [],
                  "undersporsmal": [
                    {
                      "id": "90465958-d655-3f50-bf60-b6f64cc6aa58",
                      "tag": "INNTEKTSKILDE_SELVSTENDIG_DAGMAMMA_ER_DU_SYKMELDT",
                      "sporsmalstekst": "Er du sykmeldt fra dette?",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "JA_NEI",
                      "kriterieForVisningAvUndersporsmal": null,
                      "svar": [],
                      "undersporsmal": []
                    }
                  ]
                },
                {
                  "id": "c4395282-93e5-3b11-ab5a-0a8dd2ba4f48",
                  "tag": "INNTEKTSKILDE_JORDBRUKER",
                  "sporsmalstekst": "jordbruk / fiske / reindrift",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "CHECKBOX",
                  "kriterieForVisningAvUndersporsmal": "CHECKED",
                  "svar": [],
                  "undersporsmal": [
                    {
                      "id": "e018a37a-b4c4-3075-b2b5-cbf6f50f5dcf",
                      "tag": "INNTEKTSKILDE_JORDBRUKER_ER_DU_SYKMELDT",
                      "sporsmalstekst": "Er du sykmeldt fra dette?",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "JA_NEI",
                      "kriterieForVisningAvUndersporsmal": null,
                      "svar": [],
                      "undersporsmal": []
                    }
                  ]
                },
                {
                  "id": "2fd16d5b-9944-35ca-bbf3-10b987312b96",
                  "tag": "INNTEKTSKILDE_FRILANSER",
                  "sporsmalstekst": "frilanser",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "CHECKBOX",
                  "kriterieForVisningAvUndersporsmal": "CHECKED",
                  "svar": [],
                  "undersporsmal": [
                    {
                      "id": "acd37d5f-4526-3add-bfb5-a2c1e7d8fc2a",
                      "tag": "INNTEKTSKILDE_FRILANSER_ER_DU_SYKMELDT",
                      "sporsmalstekst": "Er du sykmeldt fra dette?",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "JA_NEI",
                      "kriterieForVisningAvUndersporsmal": null,
                      "svar": [],
                      "undersporsmal": []
                    }
                  ]
                },
                {
                  "id": "3f3767f9-9edc-338d-8dc6-d5ab4e5cf4c2",
                  "tag": "INNTEKTSKILDE_ANNET",
                  "sporsmalstekst": "annet",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "CHECKBOX",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                }
              ]
            }
          ]
        },
        {
          "id": "b102bfd0-b3b0-3cf0-b9ba-b936f1bbde00",
          "tag": "VAER_KLAR_OVER_AT",
          "sporsmalstekst": "Viktig å være klar over:",
          "undertekst": "<ul><li>Du kan bare få sykepenger hvis det er din egen sykdom eller skade som hindrer deg i å jobbe. Sosiale eller økonomiske problemer gir ikke rett til sykepenger.</li><li>Du kan miste retten til sykepenger hvis du nekter å opplyse om din egen arbeidsevne, eller hvis du ikke tar imot behandling eller tilrettelegging.</li><li>Retten til sykepenger gjelder bare inntekt du har mottatt som lønn og betalt skatt av på sykmeldingstidspunktet.</li><li>NAV kan innhente opplysninger som er nødvendige for å behandle søknaden.</li><li>Fristen for å søke sykepenger er som hovedregel 3 måneder.</li><li>Du kan endre svarene i denne søknaden opp til 12 måneder etter du sendte den inn første gangen.</li><li>Du må melde fra til NAV hvis du satt i varetekt, sonet straff eller var under forvaring i sykmeldingsperioden. <a href=\"https://www.nav.no/skriv-til-oss\" target=\"_blank\">Meld fra til NAV her.</a></li><li>Du må melde fra om studier som er påbegynt etter at du ble sykmeldt, og som ikke er avklart med NAV. Det samme gjelder hvis du begynner å studere mer enn du gjorde før du ble sykmeldt. <a href=\"https://www.nav.no/skriv-til-oss\" target=\"_blank\">Meld fra til NAV her.</a></li><li>Du kan lese mer om rettigheter og plikter på <a href=\"https://www.nav.no/sykepenger\" target=\"_blank\">nav.no/sykepenger</a>.</li></ul>",
          "min": null,
          "max": null,
          "svartype": "IKKE_RELEVANT",
          "kriterieForVisningAvUndersporsmal": null,
          "svar": [],
          "undersporsmal": []
        },
        {
          "id": "75ecfc0c-183c-3588-8164-3e6601dd25ae",
          "tag": "BEKREFT_OPPLYSNINGER",
          "sporsmalstekst": "Jeg har lest all informasjonen jeg har fått i søknaden og bekrefter at opplysningene jeg har gitt er korrekte.",
          "undertekst": null,
          "min": null,
          "max": null,
          "svartype": "CHECKBOX_PANEL",
          "kriterieForVisningAvUndersporsmal": null,
          "svar": [
            {
              "verdi": "CHECKED"
            }
          ],
          "undersporsmal": []
        }
      ]
}
""".trimIndent()

    @Language("JSON")
    private fun inntektsmeldingJson() = """
        {
      "inntektsmeldingId": "bc1f75ef-4d83-40e9-8c45-f3e6ae759ca7",
      "arbeidstakerFnr": "",
      "arbeidstakerAktorId": "2989970687986",
      "virksomhetsnummer": "810007842",
      "arbeidsgiverFnr": null,
      "arbeidsgiverAktorId": null,
      "innsenderFulltNavn": "MUSKULØS VALS",
      "innsenderTelefon": "12345678",
      "begrunnelseForReduksjonEllerIkkeUtbetalt": "",
      "bruttoUtbetalt": null,
      "arbeidsgivertype": "VIRKSOMHET",
      "arbeidsforholdId": null,
      "beregnetInntekt": "35000.00",
      "inntektsdato": "2023-08-01",
      "refusjon": {
        "beloepPrMnd": "0.00",
        "opphoersdato": null
      },
      "endringIRefusjoner": [],
      "opphoerAvNaturalytelser": [],
      "gjenopptakelseNaturalytelser": [],
      "arbeidsgiverperioder": [
        {
          "fom": "2023-08-01",
          "tom": "2023-08-16"
        }
      ],
      "status": "GYLDIG",
      "arkivreferanse": "im_620105684",
      "ferieperioder": [],
      "foersteFravaersdag": "2023-08-01",
      "mottattDato": "2023-11-10T13:08:17.232151212",
      "naerRelasjon": null,
      "avsenderSystem": {
        "navn": "NAV_NO",
        "versjon": "1.0"
      },
      "inntektEndringAarsak": {
        "aarsak": "Tariffendring",
        "perioder":null,
        "gjelderFra":"2023-08-08",
        "bleKjent":"2023-09-12"
      }
    }
    """.trimIndent()
}
