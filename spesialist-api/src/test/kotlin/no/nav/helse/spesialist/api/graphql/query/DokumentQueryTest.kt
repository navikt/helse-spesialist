package no.nav.helse.spesialist.api.graphql.query

import io.mockk.every
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.objectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DokumentQueryTest : AbstractGraphQLApiTest() {

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

        assertEquals(408, dokument["extensions"]["code"].asInt())
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
                    sykmeldingSkrevet, arbeidGjenopptatt, egenmeldingsdagerFraSykmelding, soknadsperioder {
                    fom, tom, grad, faktiskGrad
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

        assertEquals(5, dokument.size())
        assertEquals(arbeidGjenopptatt, dokument["arbeidGjenopptatt"].asText())
        assertEquals(sykmeldingSkrevet, dokument["sykmeldingSkrevet"].asText())
        assertEquals("2018-01-01", dokument["egenmeldingsdagerFraSykmelding"].first().asText())
        val hentetSoknadsperioder = dokument["soknadsperioder"].single()
        assertEquals("2018-01-01", hentetSoknadsperioder["fom"].asText())
        assertEquals("2018-01-31", hentetSoknadsperioder["tom"].asText())
        assertEquals(100, hentetSoknadsperioder["grad"].asInt())
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
                    sykmeldingSkrevet, arbeidGjenopptatt, egenmeldingsdagerFraSykmelding, soknadsperioder {
                    fom, tom, grad, faktiskGrad
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

        val spørsmål = dokument["sporsmal"]
        assertEquals(4, spørsmål.size())
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
    }

    @Language("JSON")
    private fun søknadJsonMedNeiSvar(arbeidGjenopptatt: String, sykmeldingSkrevet: String) = """{
  "arbeidGjenopptatt": "$arbeidGjenopptatt",
  "sykmeldingSkrevet": "$sykmeldingSkrevet",
  "egenmeldingsdagerFraSykmelding": ["2018-01-01"],
  "soknadsperioder": [{"fom": "2018-01-01", "tom": "2018-01-31", "grad": 100, "faktiskGrad": null}],
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
  "arbeidGjenopptatt": "${LocalDate.now()}",
  "sykmeldingSkrevet": "${LocalDateTime.now()}",
  "egenmeldingsdagerFraSykmelding": ["2018-01-01"],
  "soknadsperioder": [{"fom": "2018-01-01", "tom": "2018-01-31", "grad": 100, "faktiskGrad": null}],
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
          "id": "fa254129-de59-3735-a7be-cbf11865b80e",
          "tag": "ANDRE_INNTEKTSKILDER_V2",
          "sporsmalstekst": "Har du andre inntektskilder enn Pengeløs Sparebank?",
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
              "id": "b9de35ba-d98d-3274-a81d-22de07f12c25",
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
                  "id": "de2cb7d3-0a64-3ffc-af46-5918b3be6f20",
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
                      "id": "baaa6d37-30e9-3db9-919d-8d2bdaad70b1",
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
                  "id": "8fd8af15-0541-34b9-8d79-3d7a14376493",
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
                      "id": "75e4c987-700c-371c-a104-5c77161c402d",
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
                          "id": "d40bcb0a-d7c2-3a3d-a698-7b269283bb00",
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
                              "id": "e36c4471-74fe-3855-a2c8-a1716a3a5c4a",
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
                              "id": "bb58f043-04bc-31fe-a75d-cc724021ed4c",
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
                              "id": "1c7c5659-3faf-3ddf-8f06-1a692edf9e7e",
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
                  "id": "e52dbdeb-adde-3ee7-897d-676a1a4dcbb3",
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
                  "id": "1191a448-ca92-3a3f-9447-8e6de5680b60",
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
                  "id": "f682d284-db14-3abc-b146-94290ae4cee0",
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
                  "id": "0b6271c3-b2af-30d6-924a-4e6cbeb7d963",
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
                  "id": "d35bbfa3-add7-3561-970e-5afa9dedaa06",
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
                  "id": "176603d4-b902-36b5-9753-3cdcb09fa0bb",
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

}