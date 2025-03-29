package no.nav.helse.spesialist.e2etests

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.test.TestPerson
import org.intellij.lang.annotations.Language
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SpleisStub(
    private val rapidsConnection: RapidsConnection,
    private val wireMockServer: WireMockServer
) : River.PacketListener {
    private val meldingsendere = ConcurrentHashMap<UUID, SpleisTestMeldingPubliserer>()

    fun simulerFremTilOgMedGodkjenningsbehov(testPerson: TestPerson, vedtaksperiodeId: UUID) {
        simulerFremTilOgMedNyUtbetaling(testPerson, vedtaksperiodeId)
        simulerFraNyUtbetalingTilOgMedGodkjenningsbehov(vedtaksperiodeId)
    }

    fun stubSnapshotForPerson(testPerson: TestPerson, vedtaksperiodeId: UUID) {
        @Language("JSON")
        val data = """
            {
                "data": {
                    "person": {
                        "aktorId": ${testPerson.aktørId},
                        "fodselsnummer": ${testPerson.fødselsnummer},
                        "dodsdato": null,
                        "versjon": 1,
                        "vilkarsgrunnlag": [],
                        "arbeidsgivere": [
                          {
                            "organisasjonsnummer": ${testPerson.orgnummer},
                            "ghostPerioder": [],
                            "nyesteInntektsforholdPerioder": [],
                            "generasjoner": [
                              {
                                "id": "${UUID.randomUUID()}",
                                "perioder": [
                                   {
                                      "behandlingId": "${UUID.randomUUID()}",
                                      "erForkastet": false,
                                      "fom": "",
                                      "tom": "",
                                      "inntektstype":  "EnArbeidsgiver",
                                      "opprettet": "",
                                      "periodetype":  "Forstegangsbehandling",
                                      "periodetilstand": "Utbetalt",
                                      "skjaeringstidspunkt":  "",
                                      "tidslinje": [],
                                      "hendelser":  [],
                                      "vedtaksperiodeId": ${vedtaksperiodeId},
                                      "beregningId":  ${UUID.randomUUID()},
                                      "forbrukteSykedager": 10,
                                      "gjenstaendeSykedager":  10,
                                      "maksdato": "",
                                      "inntekter":  [],
                                      "periodevilkar": "sykepengedager",
                                      "utbetaling": {
                                        "id": "2cbde6d4-e816-4bff-89e7-f88be30665f0",
                                        "arbeidsgiverFagsystemId": "VQRYGJ42CBCMNKYVSMAWPDCK2M",
                                        "arbeidsgiverNettoBelop": 7612,
                                        "personFagsystemId": "OQ6DK2G7DJAL7BFMV5ZE4WUAG4",
                                        "personNettoBelop": 0,
                                        "status": "UBETALT",
                                        "type": "UTBETALING",
                                        "vurdering": null,
                                        "arbeidsgiversimulering": {
                                            "fagsystemId": "VQRYGJ42CBCMNKYVSMAWPDCK2M",
                                            "totalbelop": 7612,
                                            "tidsstempel": "2025-03-28T14:19:15.837468276",
                                            "utbetalingslinjer": [],
                                            "perioder": [
                                            "utbetaling": {
                                        "id": "2cbde6d4-e816-4bff-89e7-f88be30665f0",
                                        "arbeidsgiverFagsystemId": "VQRYGJ42CBCMNKYVSMAWPDCK2M",
                                        "arbeidsgiverNettoBelop": 7612,
                                        "personFagsystemId": "OQ6DK2G7DJAL7BFMV5ZE4WUAG4",
                                        "personNettoBelop": 0,
                                        "status": "UBETALT",
                                        "type": "UTBETALING",
                                        "vurdering": null,
                                        "arbeidsgiversimulering": {
                                            "fagsystemId": "VQRYGJ42CBCMNKYVSMAWPDCK2M",
                                            "totalbelop": 7612,
                                            "tidsstempel": "2025-03-28T14:19:15.837468276",
                                            "utbetalingslinjer": [],
                                            "perioder": [
                                                {
                                                    "fom": "2024-12-17",
                                                    "tom": "2024-12-31",
                                                    "utbetalinger": [
                                                        {
                                                            "mottakerId": "947064649",
                                                            "mottakerNavn": "SJOKKERENDE ELEKTRIKER",
                                                            "forfall": "2025-03-28",
                                                            "feilkonto": false,
                                                            "detaljer": [
                                                                {
                                                                    "fom": "2024-12-17",
                                                                    "tom": "2024-12-31",
                                                                    "utbetalingstype": "YTEL",
                                                                    "uforegrad": 100,
                                                                    "typeSats": "DAG",
                                                                    "tilbakeforing": false,
                                                                    "sats": 692.0,
                                                                    "refunderesOrgNr": "947064649",
                                                                    "konto": "2338020",
                                                                    "klassekode": "SPREFAG-IOP",
                                                                    "antallSats": 11,
                                                                    "belop": 7612,
                                                                    "klassekodebeskrivelse": "Sykepenger, Refusjon arbeidsgiver"
                                                                }
                                                            ]
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        "personsimulering": {
                                            "fagsystemId": "OQ6DK2G7DJAL7BFMV5ZE4WUAG4",
                                            "totalbelop": null,
                                            "tidsstempel": "2025-03-28T14:19:15.837484965",
                                            "utbetalingslinjer": [],
                                            "perioder": null
                                        }
                                    }]
                                        },
                                        "personsimulering": {
                                            "fagsystemId": "OQ6DK2G7DJAL7BFMV5ZE4WUAG4",
                                            "totalbelop": null,
                                            "tidsstempel": "2025-03-28T14:19:15.837484965",
                                            "utbetalingslinjer": [],
                                            "perioder": null
                                        }
                                    },
                                      "vilkarsgrunnlagId": ${UUID.randomUUID()}
                                   }
                                ]
                              }
                            ]     
                          }
                        ]
                    }
                }
            }
            """.trimIndent()
        wireMockServer.stubFor(
            post("/graphql").willReturn(okJson(data))
        )
    }


    fun simulerFremTilOgMedNyUtbetaling(testPerson: TestPerson, vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = SpleisTestMeldingPubliserer(
            testPerson = testPerson,
            rapidsConnection = rapidsConnection,
            vedtaksperiodeId = vedtaksperiodeId,
        )
        meldingsendere[vedtaksperiodeId] = spleisTestMeldingPubliserer
        spleisTestMeldingPubliserer.simulerPublisertSendtSøknadNavMelding()
        spleisTestMeldingPubliserer.simulerPublisertBehandlingOpprettetMelding()
        spleisTestMeldingPubliserer.simulerPublisertVedtaksperiodeNyUtbetalingMelding()
    }

    fun simulerFraNyUtbetalingTilOgMedGodkjenningsbehov(vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = meldingsendere[vedtaksperiodeId]
            ?: error("Fant ikke spleisTestMeldingPubliserer for vedtaksperiodeId: $vedtaksperiodeId")
        spleisTestMeldingPubliserer.simulerPublisertUtbetalingEndretMelding()
        spleisTestMeldingPubliserer.simulerPublisertVedtaksperiodeEndretMelding()
        spleisTestMeldingPubliserer.simulerPublisertGodkjenningsbehovMelding()
    }

    fun simulerPublisertAktivitetsloggNyAktivitetMelding(varselkoder: List<String>, vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = meldingsendere[vedtaksperiodeId]
            ?: error("Fant ikke spleisTestMeldingPubliserer for vedtaksperiodeId: $vedtaksperiodeId")
        spleisTestMeldingPubliserer.simulerPublisertAktivitetsloggNyAktivitetMelding(varselkoder)
    }

    fun håndterUtbetalingUtbetalt(vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = meldingsendere[vedtaksperiodeId]
            ?: error("Fant ikke spleisTestMeldingPubliserer for vedtaksperiodeId: $vedtaksperiodeId")
        spleisTestMeldingPubliserer.simulerPublisertUtbetalingEndretTilUtbetaltMelding()
    }

    fun håndterAvsluttetMedVedtak(vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = meldingsendere[vedtaksperiodeId]
            ?: error("Fant ikke spleisTestMeldingPubliserer for vedtaksperiodeId: $vedtaksperiodeId")
        spleisTestMeldingPubliserer.simulerPublisertAvsluttetMedVedtakMelding()
    }

    fun simulerPublisertGosysOppgaveEndretMelding(vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = meldingsendere[vedtaksperiodeId]
            ?: error("Fant ikke spleisTestMeldingPubliserer for vedtaksperiodeId: $vedtaksperiodeId")
        spleisTestMeldingPubliserer.simulerPublisertGosysOppgaveEndretMelding()
    }

    fun registerOn(rapidsConnection: RapidsConnection) {
        River(rapidsConnection)
            .precondition(::precondition)
            .register(this)
    }

    private fun precondition(jsonMessage: JsonMessage) {
        jsonMessage.requireAll("@behov", listOf("Godkjenning"))
        jsonMessage.requireKey("@løsning")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val jsonNode = objectMapper.readTree(packet.toJson())
        val vedtaksperiodeId = UUID.fromString(jsonNode["vedtaksperiodeId"].asText())
        val godkjent = jsonNode["@løsning"]["Godkjenning"]["godkjent"].asBoolean()
        if (godkjent) {
            håndterUtbetalingUtbetalt(vedtaksperiodeId)
            håndterAvsluttetMedVedtak(vedtaksperiodeId)
        } else {
            logg.warn("Mottok godkjent = false i løsning på Godkjenning, håndterer ikke dette per nå")
        }
    }
}
