package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
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
import no.nav.helse.spesialist.e2etests.context.Sykepengegrunnlagsfakta
import no.nav.helse.spesialist.e2etests.context.TestContext
import no.nav.helse.spesialist.e2etests.context.Vedtaksperiode
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SpleisStub(
    private val rapidsConnection: RapidsConnection,
    private val wireMockServer: WireMockServer
) {
    private val contextsForFødselsnummer = ConcurrentHashMap<String, TestContext>()

    fun init(context: TestContext) {
        contextsForFødselsnummer[context.person.fødselsnummer] = context
    }

    fun stubSnapshotForPerson(context: TestContext) {
        val data = javaClass.getResourceAsStream("/hentSnapshot.json").use(objectMapper::readTree)

        val person = context.person

        settVerdi(data, "/data/person/aktorId", person.aktørId)
        settVerdi(data, "/data/person/fodselsnummer", person.fødselsnummer)

        sequenceOf(
            "/data/person/arbeidsgivere/0/organisasjonsnummer",
            "/data/person/vilkarsgrunnlag/0/inntekter/0/arbeidsgiver",
            "/data/person/vilkarsgrunnlag/0/arbeidsgiverrefusjoner/0/arbeidsgiver",
        ).forEach {
            settVerdi(
                jsonNode = data,
                pointer = it,
                verdi = context.arbeidsgiver.organisasjonsnummer,
            )
        }

        settVerdi(
            jsonNode = data,
            pointer = "/data/person/vilkarsgrunnlag/0/id",
            verdi = context.vilkårsgrunnlagId.toString()
        )

        context.vedtaksperioder.forEachIndexed { index, vedtaksperiode ->
            if (index > 0) {
                // Opprett kopi av periodeelementer
                val perioder =
                    data.at(JsonPointer.compile("/data/person/arbeidsgivere/0/generasjoner/0/perioder")) as ArrayNode
                perioder.add(perioder[0].deepCopy<JsonNode>())
            }

            sequenceOf(
                "/data/person/arbeidsgivere/0/generasjoner/0/perioder/$index/utbetaling/arbeidsgiveroppdrag/simulering/perioder/0/utbetalinger/0/utbetalesTilId",
                "/data/person/arbeidsgivere/0/generasjoner/0/perioder/$index/utbetaling/arbeidsgiveroppdrag/simulering/perioder/0/utbetalinger/0/detaljer/0/refunderesOrgNr",
                "/data/person/arbeidsgivere/0/generasjoner/0/perioder/$index/inntekter/0/inntektskilde",
            ).forEach {
                settVerdi(
                    jsonNode = data,
                    pointer = it,
                    verdi = context.arbeidsgiver.organisasjonsnummer,
                )
            }
            settVerdi(
                jsonNode = data,
                pointer = "/data/person/arbeidsgivere/0/generasjoner/0/perioder/$index/utbetaling/arbeidsgiveroppdrag/simulering/perioder/0/utbetalinger/0/utbetalesTilNavn",
                verdi = context.arbeidsgiver.navn
            )

            settVerdi(
                jsonNode = data,
                pointer = "/data/person/arbeidsgivere/0/generasjoner/0/perioder/$index/behandlingId",
                verdi = vedtaksperiode.spleisBehandlingId.toString()
            )
            settVerdi(
                jsonNode = data,
                pointer = "/data/person/arbeidsgivere/0/generasjoner/0/perioder/$index/vedtaksperiodeId",
                verdi = vedtaksperiode.vedtaksperiodeId.toString()
            )
            settVerdi(
                jsonNode = data,
                pointer = "/data/person/arbeidsgivere/0/generasjoner/0/perioder/$index/vilkarsgrunnlagId",
                verdi = context.vilkårsgrunnlagId.toString()
            )
            sequenceOf(
                "/data/person/arbeidsgivere/0/generasjoner/0/perioder/$index/beregningId",
                "/data/person/arbeidsgivere/0/generasjoner/0/perioder/$index/utbetaling/id"
            ).forEach {
                settVerdi(
                    jsonNode = data,
                    pointer = it,
                    verdi = vedtaksperiode.utbetalingId.toString()
                )
            }
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
        GodkjenningsbehovløsningRiver(rapidsConnection)
        OverstyringRiver(rapidsConnection)
    }

    private inner class GodkjenningsbehovløsningRiver(rapidsConnection: RapidsConnection) : River.PacketListener {
        init {
            River(rapidsConnection)
                .precondition(::precondition)
                .register(this)
        }
        override fun onPacket(
            packet: JsonMessage,
            context: MessageContext,
            metadata: MessageMetadata,
            meterRegistry: MeterRegistry
        ) {
            val jsonNode = objectMapper.readTree(packet.toJson())
            if (jsonNode["@løsning"]["Godkjenning"]["godkjent"].asBoolean()) {
                val fødselsnummer = jsonNode["fødselsnummer"].asText()
                val testContext = contextsForFødselsnummer[fødselsnummer]
                    ?: error("Ikke initialisert med context for person $fødselsnummer")
                val vedtaksperiodeId = UUID.fromString(jsonNode["vedtaksperiodeId"].asText())
                val vedtaksperiode = testContext.vedtaksperioder.find { it.vedtaksperiodeId == vedtaksperiodeId }
                    ?: error("Fant ikke igjen vedtaksperiode $vedtaksperiodeId i context for person $fødselsnummer")
                utbetalingSkjer(vedtaksperiode, testContext.person, testContext.arbeidsgiver)
                spleisAvslutterPerioden(vedtaksperiode, testContext.person, testContext.arbeidsgiver)
            } else {
                logg.warn("Mottok godkjent = false i løsning på Godkjenning, håndterer ikke dette per nå")
            }
        }

        private fun precondition(jsonMessage: JsonMessage) {
            jsonMessage.requireAll("@behov", listOf("Godkjenning"))
            jsonMessage.requireKey("@løsning")
        }

        private fun utbetalingSkjer(
            vedtaksperiode: Vedtaksperiode,
            person: Person,
            arbeidsgiver: Arbeidsgiver
        ) {
            rapidsConnection.publish(
                person.fødselsnummer,
                Meldingsbygger.byggUtbetalingEndret(
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
            rapidsConnection.publish(
                person.fødselsnummer,
                Meldingsbygger.byggAvsluttetMedVedtak(person, arbeidsgiver, vedtaksperiode)
            )
        }
    }

    private inner class OverstyringRiver(rapidsConnection: RapidsConnection) : River.PacketListener {
        init {
            River(rapidsConnection)
                .precondition(::precondition)
                .register(this)
        }

        override fun onPacket(
            packet: JsonMessage,
            context: MessageContext,
            metadata: MessageMetadata,
            meterRegistry: MeterRegistry
        ) {
            val jsonNode = objectMapper.readTree(packet.toJson())
            val skjønnsfastsatteArbeidsgivere = jsonNode["arbeidsgivere"]
            val skjæringstidspunkt = jsonNode["skjæringstidspunkt"].asLocalDate()
            val fødselsnummer = jsonNode["fødselsnummer"].asText()
            val testContext = contextsForFødselsnummer[fødselsnummer]
                ?: error("Ikke initialisert med context for person $fødselsnummer")
            val vedtaksperiode = testContext.vedtaksperioder
                .find { it.skjæringstidspunkt == skjæringstidspunkt }
                ?: error("Fant ikke vedtaksperiode med skjæringstidspunkt $skjæringstidspunkt i context for person $fødselsnummer")
            spleisRespondererPåOverstyring(testContext, vedtaksperiode)
            spleisSenderGodkjenningsbehovMedSkjønnsfastsattSykepengegrunnlag(
                person = testContext.person,
                arbeidsgiver = testContext.arbeidsgiver,
                vedtaksperiode = vedtaksperiode,
                vilkårsgrunnlagId = testContext.vilkårsgrunnlagId,
                skjønnsfastsatteArbeidsgivereJson = skjønnsfastsatteArbeidsgivere
            )
        }

        private fun precondition(jsonMessage: JsonMessage) {
            jsonMessage.requireValue("@event_name", "skjønnsmessig_fastsettelse")
        }

        private fun spleisRespondererPåOverstyring(
            testContext: TestContext,
            vedtaksperiode: Vedtaksperiode
        ) {
            spleisReberegnerPerioden(testContext.person, vedtaksperiode)
            spleisForkasterGammelUtbetaling(testContext.person, testContext.arbeidsgiver, vedtaksperiode)
            spleisLagerNyUtbetalingForVedtaksperiode(testContext.person, testContext.arbeidsgiver, vedtaksperiode)
        }

        private fun spleisReberegnerPerioden(person: Person, vedtaksperiode: Vedtaksperiode) {
            val melding = Meldingsbygger.byggVedtaksperiodeEndret(
                vedtaksperiode = vedtaksperiode,
                person = person,
                forrigeTilstand = "AVVENTER_GODKJENNING",
                gjeldendeTilstand = "AVVENTER_BLOKKERENDE_PERIODE"
            )
            rapidsConnection.publish(person.fødselsnummer, melding)
        }

        private fun spleisForkasterGammelUtbetaling(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode
        ) {
            val melding = Meldingsbygger.byggUtbetalingEndret(
                vedtaksperiode = vedtaksperiode,
                person = person,
                arbeidsgiver = arbeidsgiver,
                forrigeStatus = "IKKE_UTBETALT",
                gjeldendeStatus = "FORKASTET"
            )
            vedtaksperiode.utbetalingId = null
            rapidsConnection.publish(person.fødselsnummer, melding)
        }

        private fun spleisLagerNyUtbetalingForVedtaksperiode(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode
        ) {
            vedtaksperiode.nyUtbetaling()
            val utbetalingEndret = Meldingsbygger.byggUtbetalingEndret(
                vedtaksperiode = vedtaksperiode,
                person = person,
                arbeidsgiver = arbeidsgiver,
                forrigeStatus = "NY",
                gjeldendeStatus = "IKKE_UTBETALT",
            )
            val vedtaksperiodeNyUtbetaling = Meldingsbygger.byggVedtaksperiodeNyUtbetaling(
                vedtaksperiode = vedtaksperiode,
                person = person,
                arbeidsgiver = arbeidsgiver,
            )
            rapidsConnection.publish(person.fødselsnummer, utbetalingEndret)
            rapidsConnection.publish(person.fødselsnummer, vedtaksperiodeNyUtbetaling)
        }

        private fun spleisSenderGodkjenningsbehovMedSkjønnsfastsattSykepengegrunnlag(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            vilkårsgrunnlagId: UUID,
            skjønnsfastsatteArbeidsgivereJson: JsonNode
        ) {
            vedtaksperiode.sykepengegrunnlagsfakta = Sykepengegrunnlagsfakta(
                skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt,
                fastsatt = Sykepengegrunnlagsfakta.FastsattType.EtterSkjønn,
                arbeidsgivere = skjønnsfastsatteArbeidsgivereJson.map {
                    Sykepengegrunnlagsfakta.SkjønnsfastsattArbeidsgiver(
                        organisasjonsnummer = it["organisasjonsnummer"].asText(),
                        omregnetÅrsinntekt = 123456.7,
                        skjønnsfastsatt = it["årlig"].asDouble()
                    )
                }
            )
            val melding = Meldingsbygger.byggGodkjenningsbehov(
                person = person,
                arbeidsgiver = arbeidsgiver,
                vedtaksperiode = vedtaksperiode,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                sykepengegrunnlagsfakta = vedtaksperiode.sykepengegrunnlagsfakta,
            )
            rapidsConnection.publish(person.fødselsnummer, melding)
        }
    }
}
