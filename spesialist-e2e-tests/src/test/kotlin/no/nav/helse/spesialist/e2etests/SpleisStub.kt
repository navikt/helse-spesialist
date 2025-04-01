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
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.e2etests.context.Arbeidsgiver
import no.nav.helse.spesialist.e2etests.context.Person
import no.nav.helse.spesialist.e2etests.context.TestContext
import no.nav.helse.spesialist.e2etests.context.Vedtaksperiode
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SpleisStub(
    private val rapidsConnection: RapidsConnection,
    private val wireMockServer: WireMockServer
) : River.PacketListener {
    private val contextsForFødselsnummer = ConcurrentHashMap<String, TestContext>()

    fun init(context: TestContext) {
        contextsForFødselsnummer[context.person.fødselsnummer] = context
    }

    fun stubSnapshotForPerson(personContext: TestContext) {
        val data = javaClass.getResourceAsStream("/hentSnapshot.json").use(objectMapper::readTree)

        val person = personContext.person

        settVerdi(data, "/data/person/aktorId", person.aktørId)
        settVerdi(data, "/data/person/fodselsnummer", person.fødselsnummer)

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
                verdi = personContext.arbeidsgiver.organisasjonsnummer,
            )
        }
        settVerdi(
            jsonNode = data,
            pointer = "/data/person/arbeidsgivere/0/generasjoner/0/perioder/0/utbetaling/arbeidsgiveroppdrag/simulering/perioder/0/utbetalinger/0/utbetalesTilNavn",
            verdi = personContext.arbeidsgiver.navn
        )

        val vedtaksperiode = personContext.vedtaksperioder.first()
        settVerdi(
            jsonNode = data,
            pointer = "/data/person/arbeidsgivere/0/generasjoner/0/perioder/0/behandlingId",
            verdi = vedtaksperiode.spleisBehandlingId.toString()
        )
        settVerdi(
            jsonNode = data,
            pointer = "/data/person/arbeidsgivere/0/generasjoner/0/perioder/0/vedtaksperiodeId",
            verdi = vedtaksperiode.vedtaksperiodeId.toString()
        )
        sequenceOf(
            "/data/person/vilkarsgrunnlag/0/id",
            "/data/person/arbeidsgivere/0/generasjoner/0/perioder/0/vilkarsgrunnlagId"
        ).forEach {
            settVerdi(
                jsonNode = data,
                pointer = it,
                verdi = vedtaksperiode.vilkårsgrunnlagId.toString()
            )
        }
        sequenceOf(
            "/data/person/arbeidsgivere/0/generasjoner/0/perioder/0/beregningId",
            "/data/person/arbeidsgivere/0/generasjoner/0/perioder/0/utbetaling/id"
        ).forEach {
            settVerdi(
                jsonNode = data,
                pointer = it,
                verdi = vedtaksperiode.utbetalingId.toString()
            )
        }

        wireMockServer.stubFor(
            post("/graphql")
                .withRequestBody(matchingJsonPath("\$.variables[?(@.fnr == '${person.fødselsnummer}')]"))
                .willReturn(okJson(data.toPrettyString()))
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
        val godkjent = jsonNode["@løsning"]["Godkjenning"]["godkjent"].asBoolean()
        if (godkjent) {
            val fødselsnummer = jsonNode["fødselsnummer"].asText()
            val testContext = contextsForFødselsnummer[fødselsnummer] ?: error("Ikke initialisert med context for person $fødselsnummer")
            val vedtaksperiodeId = UUID.fromString(jsonNode["vedtaksperiodeId"].asText())
            val vedtaksperiode = testContext.vedtaksperioder.find { it.vedtaksperiodeId == vedtaksperiodeId }
                ?: error("Fant ikke igjen vedtaksperiode $vedtaksperiodeId i context for person $fødselsnummer")
            utbetalingSkjer(vedtaksperiode, testContext.person, testContext.arbeidsgiver)
            spleisAvslutterPerioden(vedtaksperiode, testContext.person, testContext.arbeidsgiver)
        } else {
            logg.warn("Mottok godkjent = false i løsning på Godkjenning, håndterer ikke dette per nå")
        }
    }

    private fun utbetalingSkjer(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        arbeidsgiver: Arbeidsgiver
    ) {
        rapidsConnection.publish(
            VårMeldingsbygger.byggUtbetalingEndret(
                vedtaksperiode = vedtaksperiode,
                person = person,
                arbeidsgiver = arbeidsgiver,
                forrigeStatus = "SENDT",
                gjeldendeStatus = "UTBETALT"
            )
        )
    }

    private fun spleisAvslutterPerioden(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        arbeidsgiver: Arbeidsgiver
    ) {
        rapidsConnection.publish(VårMeldingsbygger.byggAvsluttetMedVedtak(person, arbeidsgiver, vedtaksperiode))
    }
}
