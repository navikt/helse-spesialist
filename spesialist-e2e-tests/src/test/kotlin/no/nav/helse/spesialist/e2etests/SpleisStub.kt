package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SpleisStub(
    private val rapidsConnection: RapidsConnection,
    private val wireMockServer: WireMockServer
) : River.PacketListener {
    private val meldingsendere = ConcurrentHashMap<UUID, SpleisTestMeldingPubliserer>()

    fun init(testPerson: TestPerson, vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = SpleisTestMeldingPubliserer(
            testPerson = testPerson,
            rapidsConnection = rapidsConnection,
            vedtaksperiodeId = vedtaksperiodeId,
        )
        meldingsendere[vedtaksperiodeId] = spleisTestMeldingPubliserer

        stubSnapshotForPerson(vedtaksperiodeId)
    }

    fun simulerFremTilOgMedGodkjenningsbehov(testPerson: TestPerson, vedtaksperiodeId: UUID) {
        simulerFremTilOgMedNyUtbetaling(testPerson, vedtaksperiodeId)
        simulerFraNyUtbetalingTilOgMedGodkjenningsbehov(vedtaksperiodeId)
    }

    fun stubSnapshotForPerson(vedtaksperiodeId: UUID) {
        val data = javaClass.getResourceAsStream("/hentSnapshot.json").use(objectMapper::readTree)

        val meldingssender = meldingsendere[vedtaksperiodeId]
            ?: error("Fant ikke meldingssender for vedtaksperiodeId: $vedtaksperiodeId")
        val testPerson = meldingssender.testPerson

        settVerdi(data, "/data/person/aktorId", testPerson.aktørId)
        settVerdi(data, "/data/person/fodselsnummer", testPerson.fødselsnummer)
        settVerdi(
            jsonNode = data,
            pointer = "/data/person/arbeidsgivere/0/generasjoner/0/perioder/0/utbetaling/arbeidsgiveroppdrag/simulering/perioder/0/utbetalinger/0/utbetalesTilNavn",
            verdi = testPerson.arbeidsgiver1.organisasjonsnavn
        )
        settVerdi(
            jsonNode = data,
            pointer = "/data/person/arbeidsgivere/0/generasjoner/0/perioder/0/vedtaksperiodeId",
            verdi = vedtaksperiodeId.toString()
        )
        settVerdi(
            jsonNode = data,
            pointer = "/data/person/arbeidsgivere/0/generasjoner/0/perioder/0/behandlingId",
            verdi = meldingssender.spleisBehandlingId.toString()
        )
        sequenceOf(
            "/data/person/arbeidsgivere/0/organisasjonsnummer",
            "/data/person/arbeidsgivere/0/generasjoner/0/perioder/0/utbetaling/arbeidsgiveroppdrag/simulering/perioder/0/utbetalinger/0/utbetalesTilId",
            "/data/person/arbeidsgivere/0/generasjoner/0/perioder/0/utbetaling/arbeidsgiveroppdrag/simulering/perioder/0/utbetalinger/0/detaljer/0/refunderesOrgNr",
            "/data/person/arbeidsgivere/0/generasjoner/0/perioder/0/inntekter/0/inntektskilde",
            "/data/person/vilkarsgrunnlag/0/inntekter/0/arbeidsgiver",
            "/data/person/vilkarsgrunnlag/0/arbeidsgiverrefusjoner/0/arbeidsgiver",
        ).forEach {
            settVerdi(
                jsonNode = data,
                pointer = it,
                verdi = testPerson.arbeidsgiver1.organisasjonsnummer,
            )
        }

        wireMockServer.stubFor(
            post("/graphql").willReturn(okJson(data.toPrettyString()))
        )
    }

    private fun settVerdi(jsonNode: JsonNode, pointer: String, verdi: String) {
        val jsonPointer = JsonPointer.compile(pointer)
        jsonNode.at(jsonPointer.head()).let {
            if (it.isMissingNode) {
                error("Fant ikke node for $jsonPointer")
            } else {
                (it as ObjectNode).put(jsonPointer.last().matchingProperty, verdi)
            }
        }
    }


    fun simulerFremTilOgMedNyUtbetaling(testPerson: TestPerson, vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = meldingsendere[vedtaksperiodeId]
            ?: error("Fant ikke spleisTestMeldingPubliserer for vedtaksperiodeId: $vedtaksperiodeId")
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
