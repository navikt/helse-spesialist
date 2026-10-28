package no.nav.helse.spesialist.e2etests.tests

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

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
                "arbeidsgivere.behandlinger.perioder.vedtaksperiodeId",
                "arbeidsgivere.behandlinger.perioder.utbetaling.id",
                "arbeidsgivere.behandlinger.perioder.vilkarsgrunnlagId",
                "arbeidsgivere.behandlinger.perioder.behandlingId",
                "arbeidsgivere.behandlinger.perioder.oppgave.id",
                "arbeidsgivere.behandlinger.perioder.varsler.id",
                "arbeidsgivere.behandlinger.perioder.varsler.behandlingId",
                "arbeidsgivere.behandlinger.perioder.varsler.opprettet",
                "arbeidsgivere.behandlinger.perioder.varsler.definisjonId",
                "arbeidsgivere.behandlinger.perioder.varsler.tittel",
                "arbeidsgivere.behandlinger.perioder.varsler.forklaring",
                "arbeidsgivere.behandlinger.perioder.varsler.handling",
            )
        }
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `auditlogger oppslag i saksbildet`() {
        // Given:
        søknadOgGodkjenningbehovKommerInn()
        val auditlogg =
            ListAppender<ILoggingEvent>().apply {
                (LoggerFactory.getLogger("auditLogger") as Logger).addAppender(this)
                start()
            }

        medPersonISpeil {
            auditlogg.list.clear()

            // When:
            hentOppdatertPerson()
        }

        // Then:
        val innslag = auditlogg.list.filter { "duid=${testContext.person.fødselsnummer}" in it.message }
        assertEquals(1, innslag.size)
        assertEquals(Level.INFO, innslag.single().level)
        assertTrue("suid=${saksbehandlerIdent().value}" in innslag.single().message)
    }

    // language=json
    private val forventetPerson =
        """
        {
          "aktorId" : "${testContext.person.aktørId}",
          "fodselsnummer" : "${testContext.person.fødselsnummer}",
          "andreFodselsnummer" : [ ],
          "dodsdato" : null,
          "tildeling" : null,
          "tilleggsinfoForInntektskilder" : [ {
            "orgnummer" : "${testContext.arbeidsgiver.organisasjonsnummer}",
            "navn" : "Navn for ${testContext.arbeidsgiver.organisasjonsnummer}"
          } ],
          "arbeidsgivere" : [ {
            "organisasjonsnummer" : "${testContext.arbeidsgiver.organisasjonsnummer}",
            "navn" : "Navn for ${testContext.arbeidsgiver.organisasjonsnummer}",
            "ghostPerioder" : [ ],
            "behandlinger" : [ {
              "id" : "0fa4aa13-9d25-4de5-92c1-4713f913330f",
              "perioder" : [ {
                "type" : "ApiBeregnetPeriode",
                "erForkastet" : false,
                "fom" : "2024-12-01",
                "tom" : "2024-12-31",
                "inntektstype" : "ENARBEIDSGIVER",
                "opprettet" : "2025-03-28T14:19:13.942271831",
                "periodetype" : "FORSTEGANGSBEHANDLING",
                "tidslinje" : [ {
                  "dato" : "2024-12-01",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYK_HELGEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-02",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-03",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-04",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-05",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-06",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-07",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYK_HELGEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-08",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYK_HELGEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-09",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-10",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-11",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-12",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-13",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-14",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYK_HELGEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-15",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYK_HELGEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-16",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "ARBEIDSGIVERPERIODEDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-17",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "NAVDAG",
                  "utbetalingsinfo" : {
                    "arbeidsgiverbelop" : 692,
                    "inntekt" : null,
                    "personbelop" : 0,
                    "refusjonsbelop" : null,
                    "totalGrad" : 100.0,
                    "utbetaling" : 692
                  },
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-18",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "NAVDAG",
                  "utbetalingsinfo" : {
                    "arbeidsgiverbelop" : 692,
                    "inntekt" : null,
                    "personbelop" : 0,
                    "refusjonsbelop" : null,
                    "totalGrad" : 100.0,
                    "utbetaling" : 692
                  },
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-19",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "NAVDAG",
                  "utbetalingsinfo" : {
                    "arbeidsgiverbelop" : 692,
                    "inntekt" : null,
                    "personbelop" : 0,
                    "refusjonsbelop" : null,
                    "totalGrad" : 100.0,
                    "utbetaling" : 692
                  },
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-20",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "NAVDAG",
                  "utbetalingsinfo" : {
                    "arbeidsgiverbelop" : 692,
                    "inntekt" : null,
                    "personbelop" : 0,
                    "refusjonsbelop" : null,
                    "totalGrad" : 100.0,
                    "utbetaling" : 692
                  },
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-21",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYK_HELGEDAG",
                  "utbetalingsdagtype" : "NAVHELGDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-22",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYK_HELGEDAG",
                  "utbetalingsdagtype" : "NAVHELGDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-23",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "NAVDAG",
                  "utbetalingsinfo" : {
                    "arbeidsgiverbelop" : 692,
                    "inntekt" : null,
                    "personbelop" : 0,
                    "refusjonsbelop" : null,
                    "totalGrad" : 100.0,
                    "utbetaling" : 692
                  },
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-24",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "NAVDAG",
                  "utbetalingsinfo" : {
                    "arbeidsgiverbelop" : 692,
                    "inntekt" : null,
                    "personbelop" : 0,
                    "refusjonsbelop" : null,
                    "totalGrad" : 100.0,
                    "utbetaling" : 692
                  },
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-25",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "NAVDAG",
                  "utbetalingsinfo" : {
                    "arbeidsgiverbelop" : 692,
                    "inntekt" : null,
                    "personbelop" : 0,
                    "refusjonsbelop" : null,
                    "totalGrad" : 100.0,
                    "utbetaling" : 692
                  },
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-26",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "NAVDAG",
                  "utbetalingsinfo" : {
                    "arbeidsgiverbelop" : 692,
                    "inntekt" : null,
                    "personbelop" : 0,
                    "refusjonsbelop" : null,
                    "totalGrad" : 100.0,
                    "utbetaling" : 692
                  },
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-27",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "NAVDAG",
                  "utbetalingsinfo" : {
                    "arbeidsgiverbelop" : 692,
                    "inntekt" : null,
                    "personbelop" : 0,
                    "refusjonsbelop" : null,
                    "totalGrad" : 100.0,
                    "utbetaling" : 692
                  },
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-28",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYK_HELGEDAG",
                  "utbetalingsdagtype" : "NAVHELGDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-29",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYK_HELGEDAG",
                  "utbetalingsdagtype" : "NAVHELGDAG",
                  "utbetalingsinfo" : null,
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-30",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "NAVDAG",
                  "utbetalingsinfo" : {
                    "arbeidsgiverbelop" : 692,
                    "inntekt" : null,
                    "personbelop" : 0,
                    "refusjonsbelop" : null,
                    "totalGrad" : 100.0,
                    "utbetaling" : 692
                  },
                  "begrunnelser" : null
                }, {
                  "dato" : "2024-12-31",
                  "grad" : 100.0,
                  "kilde" : {
                    "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                    "type" : "SOKNAD"
                  },
                  "sykdomsdagtype" : "SYKEDAG",
                  "utbetalingsdagtype" : "NAVDAG",
                  "utbetalingsinfo" : {
                    "arbeidsgiverbelop" : 692,
                    "inntekt" : null,
                    "personbelop" : 0,
                    "refusjonsbelop" : null,
                    "totalGrad" : 100.0,
                    "utbetaling" : 692
                  },
                  "begrunnelser" : null
                } ],
                "periodetilstand" : "TilGodkjenning",
                "skjaeringstidspunkt" : "2024-12-01",
                "varsler" : [ {
                  "kode" : "SB_RV_1",
                  "vurdering" : null
                } ],
                "hendelser" : [ {
                  "type" : "ApiSoknadNav",
                  "id" : "af4656ef-fb9b-4c9d-b575-64242b851c5b",
                  "hendelsetype" : "SENDT_SOKNAD_NAV",
                  "fom" : "2024-12-01",
                  "tom" : "2024-12-31",
                  "rapportertDato" : "2025-01-01T00:00:00",
                  "sendtNav" : "2025-01-01T00:00:00",
                  "eksternDokumentId" : "5ff50d4f-4fb1-44ed-8dde-9cac6a1c19be"
                }, {
                  "type" : "ApiInntektsmelding",
                  "id" : "b9d11351-4b71-4c44-9708-b43fe1d6453f",
                  "hendelsetype" : "INNTEKTSMELDING",
                  "mottattDato" : "2024-12-01T00:00:00",
                  "beregnetInntekt" : 15000.0,
                  "eksternDokumentId" : "8dc8586a-958e-404c-b8f1-6285d229c407"
                } ],
                "historikkinnslag" : [ ],
                "grunnbelopsjustertGjennomsnitt" : null,
                "oppgave" : { },
                "handlinger" : [ {
                  "type" : "UTBETALE",
                  "tillatt" : true,
                  "begrunnelse" : null
                }, {
                  "type" : "AVVISE",
                  "tillatt" : true,
                  "begrunnelse" : null
                } ],
                "egenskaper" : [ {
                  "egenskap" : "SOKNAD",
                  "kategori" : "Oppgavetype"
                }, {
                  "egenskap" : "RISK_QA",
                  "kategori" : "Ukategorisert"
                }, {
                  "egenskap" : "UTBETALING_TIL_ARBEIDSGIVER",
                  "kategori" : "Mottaker"
                }, {
                  "egenskap" : "EN_ARBEIDSGIVER",
                  "kategori" : "Inntektskilde"
                }, {
                  "egenskap" : "ARBEIDSTAKER",
                  "kategori" : "Inntektsforhold"
                }, {
                  "egenskap" : "FORSTEGANGSBEHANDLING",
                  "kategori" : "Periodetype"
                } ],
                "forbrukteSykedager" : 11,
                "gjenstaendeSykedager" : 237,
                "maksdato" : "2025-11-27",
                "periodevilkar" : {
                  "alder" : {
                    "alderSisteSykedag" : 52,
                    "oppfylt" : true
                  },
                  "sykepengedager" : {
                    "forbrukteSykedager" : 11,
                    "gjenstaendeSykedager" : 237,
                    "maksdato" : "2025-11-27",
                    "oppfylt" : true,
                    "skjaeringstidspunkt" : "2024-12-01"
                  }
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
                    "tidsstempel" : "2025-03-28T14:19:15.837468276",
                    "utbetalingslinjer" : [ ],
                    "totalbelop" : 7612,
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
                          "belop" : 7612,
                          "antallSats" : 11,
                          "klassekode" : "SPREFAG-IOP",
                          "klassekodebeskrivelse" : "Sykepenger, Refusjon arbeidsgiver",
                          "konto" : "2338020",
                          "refunderesOrgNr" : "${testContext.arbeidsgiver.organisasjonsnummer}",
                          "sats" : 692.0,
                          "tilbakeforing" : false,
                          "typeSats" : "DAG",
                          "uforegrad" : 100,
                          "utbetalingstype" : "YTEL"
                        } ]
                      } ]
                    } ]
                  },
                  "personsimulering" : {
                    "fagsystemId" : "OQ6DK2G7DJAL7BFMV5ZE4WUAG4",
                    "tidsstempel" : "2025-03-28T14:19:15.837484965",
                    "utbetalingslinjer" : [ ],
                    "totalbelop" : null,
                    "perioder" : null
                  }
                },
                "risikovurdering" : {
                  "funn" : [ ],
                  "kontrollertOk" : [ ]
                },
                "totrinnsvurdering" : null,
                "paVent" : null,
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
                "annulleringskandidater" : [ ]
              } ]
            } ],
            "overstyringer" : [ ],
            "arbeidsforhold" : [ {
              "stillingstittel" : "en-stillingstittel",
              "stillingsprosent" : 100,
              "startdato" : "${LocalDate.now()}",
              "sluttdato" : null
            } ],
            "inntekterFraAordningen" : [ {
              "skjaeringstidspunkt" : "2018-01-01",
              "inntekter" : [ {
                "maned" : "${YearMonth.now().minusMonths(1)}",
                "sum" : 20000.0
              } ]
            } ]
          } ],
          "selvstendigNaering" : null,
          "vilkarsgrunnlagV2" : [ {
            "type" : "ApiVilkårsgrunnlagSpleisV2",
            "inntekter" : [ {
              "arbeidsgiver" : "${testContext.arbeidsgiver.organisasjonsnummer}",
              "omregnetArsinntekt" : {
                "belop" : 180000.0,
                "inntektFraAOrdningen" : null,
                "kilde" : "INNTEKTSMELDING",
                "manedsbelop" : 15000.0
              },
              "sammenligningsgrunnlag" : {
                "belop" : 650000.0,
                "inntektFraAOrdningen" : [ {
                  "maned" : "${YearMonth.now()}",
                  "sum" : 650000.0
                } ]
              },
              "skjonnsmessigFastsatt" : null,
              "deaktivert" : false,
              "fom" : "2024-12-01",
              "tom" : null
            } ],
            "skjaeringstidspunkt" : "2024-12-01",
            "sykepengegrunnlag" : 180000.0,
            "arbeidsgiverrefusjoner" : [ {
              "arbeidsgiver" : "${testContext.arbeidsgiver.organisasjonsnummer}",
              "refusjonsopplysninger" : [ {
                "fom" : "2024-12-01",
                "tom" : null,
                "belop" : 15000.0,
                "meldingsreferanseId" : "b9d11351-4b71-4c44-9708-b43fe1d6453f"
              } ]
            } ],
            "avviksvurdering" : {
              "avviksprosent" : "10",
              "beregningsgrunnlag" : "600000",
              "sammenligningsgrunnlag" : "650000"
            },
            "skjonnsmessigFastsattAarlig" : null,
            "antallOpptjeningsdagerErMinst" : 7919,
            "grunnbelop" : 124028,
            "sykepengegrunnlagsgrense" : {
              "grunnbelop" : 124028,
              "grense" : 744168,
              "virkningstidspunkt" : "2024-05-01"
            },
            "vurderingAvKravOmMedlemskap" : "OPPFYLT",
            "oppfyllerKravOmMinstelonn" : true,
            "oppfyllerKravOmOpptjening" : true,
            "opptjeningFra" : "2003-03-28",
            "opptjeningsvurderingId" : "b89e2ae5-59e3-388e-98cd-42a8e7350773",
            "medlemskapsvurderingId" : "4c2a8f1e-7b3d-3e5a-9c1f-2d6e8a0b4f7c",
            "beregningsgrunnlag" : "25.0",
            "forsikringsvurderingId" : null
          } ]
        }
        """.trimIndent()
}
