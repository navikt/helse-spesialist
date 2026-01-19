package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test

class HentPersonE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `henter person med forventet innhold`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            assertJsonEquals(
                forventetPerson,
                person,
                "personPseudoId",
                "vilkarsgrunnlagV2.id",
                "arbeidsgivere.behandlinger.perioder.id",
                "arbeidsgivere.behandlinger.perioder.beregningId",
                "arbeidsgivere.behandlinger.perioder.vedtaksperiodeId",
                "arbeidsgivere.behandlinger.perioder.utbetaling.id",
                "arbeidsgivere.behandlinger.perioder.vilkarsgrunnlagId",
                "arbeidsgivere.behandlinger.perioder.behandlingId",
                "arbeidsgivere.behandlinger.perioder.varsler.id",
                "arbeidsgivere.behandlinger.perioder.varsler.behandlingId",
                "arbeidsgivere.behandlinger.perioder.varsler.opprettet",
            )
        }
    }

    // language=json
    private val forventetPerson = """
    {
      "fodselsnummer" : "${testContext.person.fødselsnummer}",
      "dodsdato" : null,
      "enhet" : {
        "id" : "0301",
        "navn" : "Oslo",
        "__typename" : "Enhet"
      },
      "infotrygdutbetalinger" : [ {
        "organisasjonsnummer" : "${testContext.arbeidsgiver.organisasjonsnummer}",
        "dagsats" : 1000.0,
        "fom" : "2018-01-01",
        "tom" : "2018-01-31",
        "grad" : "100",
        "typetekst" : "ArbRef",
        "__typename" : "Infotrygdutbetaling"
      } ],
      "personinfo" : {
        "fornavn" : "${testContext.person.fornavn}",
        "mellomnavn" : ${testContext.person.mellomnavn?.let { "\"$it\"" }},
        "etternavn" : "${testContext.person.etternavn}",
        "adressebeskyttelse" : "Ugradert",
        "fodselsdato" : "${testContext.person.fødselsdato}",
        "kjonn" : "${testContext.person.kjønn}",
        "fullmakt" : false,
        "reservasjon" : {
          "kanVarsles" : true,
          "reservert" : false,
          "__typename" : "Reservasjon"
        },
        "unntattFraAutomatisering" : {
          "erUnntatt" : false,
          "arsaker" : [ ],
          "tidspunkt" : null,
          "__typename" : "UnntattFraAutomatiskGodkjenning"
        },
        "__typename" : "Personinfo"
      },
      "tildeling" : null,
      "versjon" : 54,
      "vilkarsgrunnlagV2" : [ {
        "sykepengegrunnlag" : 180000.0,
        "skjaeringstidspunkt" : "2024-12-01",
        "inntekter" : [ {
          "sammenligningsgrunnlag" : {
            "belop" : 650000.0,
            "inntektFraAOrdningen" : [ {
              "maned" : "2026-01",
              "sum" : 650000.0,
              "__typename" : "InntektFraAOrdningen"
            } ],
            "__typename" : "Sammenligningsgrunnlag"
          },
          "omregnetArsinntekt" : {
            "inntektFraAOrdningen" : null,
            "belop" : 180000.0,
            "manedsbelop" : 15000.0,
            "kilde" : "INNTEKTSMELDING",
            "__typename" : "OmregnetArsinntekt"
          },
          "skjonnsmessigFastsatt" : null,
          "arbeidsgiver" : "${testContext.arbeidsgiver.organisasjonsnummer}",
          "deaktivert" : false,
          "fom" : "2024-12-01",
          "tom" : null,
          "__typename" : "Arbeidsgiverinntekt"
        } ],
        "arbeidsgiverrefusjoner" : [ {
          "arbeidsgiver" : "${testContext.arbeidsgiver.organisasjonsnummer}",
          "refusjonsopplysninger" : [ {
            "fom" : "2024-12-01",
            "tom" : null,
            "belop" : 15000.0,
            "meldingsreferanseId" : "b9d11351-4b71-4c44-9708-b43fe1d6453f",
            "__typename" : "Refusjonselement"
          } ],
          "__typename" : "Arbeidsgiverrefusjon"
        } ],
        "skjonnsmessigFastsattAarlig" : null,
        "vurderingAvKravOmMedlemskap" : "OPPFYLT",
        "oppfyllerKravOmMinstelonn" : true,
        "oppfyllerKravOmOpptjening" : true,
        "beregningsgrunnlag" : "25.0",
        "avviksvurdering" : {
          "avviksprosent" : "10",
          "sammenligningsgrunnlag" : "650000",
          "beregningsgrunnlag" : "600000"
        },
        "antallOpptjeningsdagerErMinst" : 7919,
        "grunnbelop" : 124028,
        "opptjeningFra" : "2003-03-28",
        "sykepengegrunnlagsgrense" : {
          "grunnbelop" : 124028,
          "grense" : 744168,
          "virkningstidspunkt" : "2024-05-01",
          "__typename" : "Sykepengegrunnlagsgrense"
        },
        "__typename" : "VilkarsgrunnlagSpleisV2"
      } ],
      "aktorId" : "${testContext.person.aktørId}",
      "arbeidsgivere" : [ {
        "navn" : "Navn for ${testContext.arbeidsgiver.organisasjonsnummer}",
        "organisasjonsnummer" : "${testContext.arbeidsgiver.organisasjonsnummer}",
        "arbeidsforhold" : [ {
          "sluttdato" : null,
          "startdato" : "2026-01-19",
          "stillingsprosent" : 100,
          "stillingstittel" : "en-stillingstittel",
          "__typename" : "Arbeidsforhold"
        } ],
        "ghostPerioder" : [ ],
        "behandlinger" : [ {
          "id" : "0fa4aa13-9d25-4de5-92c1-4713f913330f",
          "perioder" : [ {
            "forbrukteSykedager" : 11,
            "gjenstaendeSykedager" : 237,
            "handlinger" : [ {
              "type" : "UTBETALE",
              "tillatt" : true,
              "begrunnelse" : null,
              "__typename" : "Handling"
            }, {
              "type" : "AVVISE",
              "tillatt" : true,
              "begrunnelse" : null,
              "__typename" : "Handling"
            } ],
            "notater" : [ ],
            "historikkinnslag" : [ ],
            "maksdato" : "2025-11-27",
            "periodevilkar" : {
              "alder" : {
                "alderSisteSykedag" : 52,
                "oppfylt" : true,
                "__typename" : "Alder"
              },
              "sykepengedager" : {
                "forbrukteSykedager" : 11,
                "gjenstaendeSykedager" : 237,
                "maksdato" : "2025-11-27",
                "oppfylt" : true,
                "skjaeringstidspunkt" : "2024-12-01",
                "__typename" : "Sykepengedager"
              },
              "__typename" : "Periodevilkar"
            },
            "risikovurdering" : {
              "funn" : [ ],
              "kontrollertOk" : [ ],
              "__typename" : "Risikovurdering"
            },
            "utbetaling" : {
              "arbeidsgiverFagsystemId" : "VQRYGJ42CBCMNKYVSMAWPDCK2M",
              "arbeidsgiverNettoBelop" : 7612,
              "personFagsystemId" : "OQ6DK2G7DJAL7BFMV5ZE4WUAG4",
              "personNettoBelop" : 0,
              "status" : "UBETALT",
              "type" : "UTBETALING",
              "vurdering" : null,
              "arbeidsgiversimulering" : {
                "fagsystemId" : "VQRYGJ42CBCMNKYVSMAWPDCK2M",
                "totalbelop" : 7612,
                "tidsstempel" : "2025-03-28T14:19:15.837468276",
                "utbetalingslinjer" : [ ],
                "perioder" : [ {
                  "fom" : "2024-12-17",
                  "tom" : "2024-12-31",
                  "utbetalinger" : [ {
                    "mottakerId" : "${testContext.arbeidsgiver.organisasjonsnummer}",
                    "mottakerNavn" : "${testContext.arbeidsgiver.navn}",
                    "forfall" : "2025-03-28",
                    "feilkonto" : false,
                    "detaljer" : [ {
                      "fom" : "2024-12-17",
                      "tom" : "2024-12-31",
                      "utbetalingstype" : "YTEL",
                      "uforegrad" : 100,
                      "typeSats" : "DAG",
                      "tilbakeforing" : false,
                      "sats" : 692.0,
                      "refunderesOrgNr" : "${testContext.arbeidsgiver.organisasjonsnummer}",
                      "konto" : "2338020",
                      "klassekode" : "SPREFAG-IOP",
                      "antallSats" : 11,
                      "belop" : 7612,
                      "klassekodebeskrivelse" : "Sykepenger, Refusjon arbeidsgiver",
                      "__typename" : "Simuleringsdetaljer"
                    } ],
                    "__typename" : "Simuleringsutbetaling"
                  } ],
                  "__typename" : "Simuleringsperiode"
                } ],
                "__typename" : "Simulering"
              },
              "personsimulering" : {
                "fagsystemId" : "OQ6DK2G7DJAL7BFMV5ZE4WUAG4",
                "totalbelop" : null,
                "tidsstempel" : "2025-03-28T14:19:15.837484965",
                "utbetalingslinjer" : [ ],
                "perioder" : null,
                "__typename" : "Simulering"
              },
              "__typename" : "Utbetaling"
            },
            "oppgave" : {
              "id" : "1",
              "__typename" : "OppgaveForPeriodevisning"
            },
            "paVent" : null,
            "totrinnsvurdering" : null,
            "egenskaper" : [ {
              "egenskap" : "SOKNAD",
              "kategori" : "Oppgavetype",
              "__typename" : "Oppgaveegenskap"
            }, {
              "egenskap" : "RISK_QA",
              "kategori" : "Ukategorisert",
              "__typename" : "Oppgaveegenskap"
            }, {
              "egenskap" : "UTBETALING_TIL_ARBEIDSGIVER",
              "kategori" : "Mottaker",
              "__typename" : "Oppgaveegenskap"
            }, {
              "egenskap" : "EN_ARBEIDSGIVER",
              "kategori" : "Inntektskilde",
              "__typename" : "Oppgaveegenskap"
            }, {
              "egenskap" : "ARBEIDSTAKER",
              "kategori" : "Inntektsforhold",
              "__typename" : "Oppgaveegenskap"
            }, {
              "egenskap" : "FORSTEGANGSBEHANDLING",
              "kategori" : "Periodetype",
              "__typename" : "Oppgaveegenskap"
            } ],
            "avslag" : [ ],
            "vedtakBegrunnelser" : [ ],
            "annullering" : null,
            "pensjonsgivendeInntekter" : [ {
              "arligBelop" : "120230.0",
              "inntektsar" : 2023
            }, {
              "arligBelop" : "220220.0",
              "inntektsar" : 2022
            }, {
              "arligBelop" : "320210.0",
              "inntektsar" : 2021
            } ],
            "annulleringskandidater" : [ ],
            "fom" : "2024-12-01",
            "tom" : "2024-12-31",
            "erForkastet" : false,
            "inntektstype" : "ENARBEIDSGIVER",
            "opprettet" : "2025-03-28T14:19:13.942271831",
            "periodetype" : "FORSTEGANGSBEHANDLING",
            "tidslinje" : [ {
              "dato" : "2024-12-01",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYK_HELGEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-02",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-03",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-04",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-05",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-06",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-07",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYK_HELGEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-08",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYK_HELGEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-09",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-10",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-11",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-12",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-13",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-14",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYK_HELGEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-15",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYK_HELGEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-16",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-17",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "NAVDAG",
              "utbetalingsinfo" : {
                "arbeidsgiverbelop" : 692,
                "inntekt" : null,
                "personbelop" : 0,
                "refusjonsbelop" : null,
                "totalGrad" : 100.0,
                "utbetaling" : 692,
                "__typename" : "Utbetalingsinfo"
              },
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-18",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "NAVDAG",
              "utbetalingsinfo" : {
                "arbeidsgiverbelop" : 692,
                "inntekt" : null,
                "personbelop" : 0,
                "refusjonsbelop" : null,
                "totalGrad" : 100.0,
                "utbetaling" : 692,
                "__typename" : "Utbetalingsinfo"
              },
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-19",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "NAVDAG",
              "utbetalingsinfo" : {
                "arbeidsgiverbelop" : 692,
                "inntekt" : null,
                "personbelop" : 0,
                "refusjonsbelop" : null,
                "totalGrad" : 100.0,
                "utbetaling" : 692,
                "__typename" : "Utbetalingsinfo"
              },
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-20",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "NAVDAG",
              "utbetalingsinfo" : {
                "arbeidsgiverbelop" : 692,
                "inntekt" : null,
                "personbelop" : 0,
                "refusjonsbelop" : null,
                "totalGrad" : 100.0,
                "utbetaling" : 692,
                "__typename" : "Utbetalingsinfo"
              },
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-21",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYK_HELGEDAG",
              "utbetalingsdagtype" : "NAVHELGDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-22",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYK_HELGEDAG",
              "utbetalingsdagtype" : "NAVHELGDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-23",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "NAVDAG",
              "utbetalingsinfo" : {
                "arbeidsgiverbelop" : 692,
                "inntekt" : null,
                "personbelop" : 0,
                "refusjonsbelop" : null,
                "totalGrad" : 100.0,
                "utbetaling" : 692,
                "__typename" : "Utbetalingsinfo"
              },
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-24",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "NAVDAG",
              "utbetalingsinfo" : {
                "arbeidsgiverbelop" : 692,
                "inntekt" : null,
                "personbelop" : 0,
                "refusjonsbelop" : null,
                "totalGrad" : 100.0,
                "utbetaling" : 692,
                "__typename" : "Utbetalingsinfo"
              },
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-25",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "NAVDAG",
              "utbetalingsinfo" : {
                "arbeidsgiverbelop" : 692,
                "inntekt" : null,
                "personbelop" : 0,
                "refusjonsbelop" : null,
                "totalGrad" : 100.0,
                "utbetaling" : 692,
                "__typename" : "Utbetalingsinfo"
              },
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-26",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "NAVDAG",
              "utbetalingsinfo" : {
                "arbeidsgiverbelop" : 692,
                "inntekt" : null,
                "personbelop" : 0,
                "refusjonsbelop" : null,
                "totalGrad" : 100.0,
                "utbetaling" : 692,
                "__typename" : "Utbetalingsinfo"
              },
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-27",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "NAVDAG",
              "utbetalingsinfo" : {
                "arbeidsgiverbelop" : 692,
                "inntekt" : null,
                "personbelop" : 0,
                "refusjonsbelop" : null,
                "totalGrad" : 100.0,
                "utbetaling" : 692,
                "__typename" : "Utbetalingsinfo"
              },
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-28",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYK_HELGEDAG",
              "utbetalingsdagtype" : "NAVHELGDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-29",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYK_HELGEDAG",
              "utbetalingsdagtype" : "NAVHELGDAG",
              "utbetalingsinfo" : null,
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-30",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "NAVDAG",
              "utbetalingsinfo" : {
                "arbeidsgiverbelop" : 692,
                "inntekt" : null,
                "personbelop" : 0,
                "refusjonsbelop" : null,
                "totalGrad" : 100.0,
                "utbetaling" : 692,
                "__typename" : "Utbetalingsinfo"
              },
              "begrunnelser" : null,
              "__typename" : "Dag"
            }, {
              "dato" : "2024-12-31",
              "grad" : 100.0,
              "kilde" : {
                "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                "type" : "SOKNAD",
                "__typename" : "Kilde"
              },
              "sykdomsdagtype" : "SYKEDAG",
              "utbetalingsdagtype" : "NAVDAG",
              "utbetalingsinfo" : {
                "arbeidsgiverbelop" : 692,
                "inntekt" : null,
                "personbelop" : 0,
                "refusjonsbelop" : null,
                "totalGrad" : 100.0,
                "utbetaling" : 692,
                "__typename" : "Utbetalingsinfo"
              },
              "begrunnelser" : null,
              "__typename" : "Dag"
            } ],
            "periodetilstand" : "TilGodkjenning",
            "skjaeringstidspunkt" : "2024-12-01",
            "varsler" : [ {
              "definisjonId" : "77970f04-c4c5-4b9f-8795-bb5e4749344c",
              "kode" : "SB_RV_1",
              "tittel" : "Faresignaler oppdaget. Kontroller om faresignalene påvirker retten til sykepenger",
              "forklaring" : null,
              "handling" : null,
              "vurdering" : null,
              "__typename" : "VarselDTO"
            } ],
            "hendelser" : [ {
              "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
              "type" : "SENDT_SOKNAD_NAV",
              "fom" : "2024-12-01",
              "tom" : "2024-12-31",
              "rapportertDato" : "2025-01-01T00:00",
              "sendtNav" : "2025-01-01T00:00",
              "eksternDokumentId" : "5ff50d4f-4fb1-44ed-8dde-9cac6a1c19be",
              "__typename" : "SoknadNav"
            }, {
              "id" : "b9d11351-4b71-4c44-9708-b43fe1d6453f",
              "type" : "INNTEKTSMELDING",
              "beregnetInntekt" : 15000.0,
              "mottattDato" : "2024-12-01T00:00",
              "eksternDokumentId" : "8dc8586a-958e-404c-b8f1-6285d229c407",
              "__typename" : "Inntektsmelding"
            } ],
            "__typename" : "BeregnetPeriode"
          } ],
          "__typename" : "Behandling"
        } ],
        "overstyringer" : [ ],
        "inntekterFraAordningen" : [ {
          "skjaeringstidspunkt" : "2018-01-01",
          "inntekter" : [ {
            "maned" : "2025-12",
            "sum" : 20000.0,
            "__typename" : "InntektFraAOrdningen"
          } ],
          "__typename" : "ArbeidsgiverInntekterFraAOrdningen"
        } ],
        "__typename" : "Arbeidsgiver"
      } ],
      "tilleggsinfoForInntektskilder" : [ {
        "orgnummer" : "${testContext.arbeidsgiver.organisasjonsnummer}",
        "navn" : "Navn for ${testContext.arbeidsgiver.organisasjonsnummer}",
        "__typename" : "TilleggsinfoForInntektskilde"
      } ],
      "__typename" : "Person"
    }
""".trimIndent()
}

