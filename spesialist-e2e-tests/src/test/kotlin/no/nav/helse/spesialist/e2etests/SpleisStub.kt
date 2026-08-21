package no.nav.helse.spesialist.e2etests

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
import io.ktor.util.collections.ConcurrentSet
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.e2etests.context.Arbeidsgiver
import no.nav.helse.spesialist.e2etests.context.Person
import no.nav.helse.spesialist.e2etests.context.Sykepengegrunnlagsfakta
import no.nav.helse.spesialist.e2etests.context.TestContext
import no.nav.helse.spesialist.e2etests.context.Vedtaksperiode
import tools.jackson.core.JsonPointer
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SpleisStub(
    private val rapidsConnection: RapidsConnection,
    private val wireMockServer: WireMockServer,
) {
    private val contextsForFødselsnummer = ConcurrentHashMap<String, TestContext>()
    private val denylisteForPersoner = ConcurrentSet<String>()

    fun init(context: TestContext) {
        contextsForFødselsnummer[context.person.fødselsnummer] = context
    }

    fun stubSnapshotForPerson(context: TestContext) {
        val mal = javaClass.getResourceAsStream("/hentSnapshot.json").use(objectMapper::readTree)

        val person = context.person
        settVerdi(mal, "/data/person/aktorId", person.aktørId)
        settVerdi(mal, "/data/person/fodselsnummer", person.fødselsnummer)

        val arbeidsgivereMal = mal.at(JsonPointer.compile("/data/person/arbeidsgivere")) as ArrayNode
        val arbeidsgiverMal = arbeidsgivereMal[0].deepCopy() as ObjectNode
        val periodeMal =
            arbeidsgiverMal
                .at(JsonPointer.compile("/generasjoner/0/perioder/0"))
                .deepCopy() as ObjectNode
        arbeidsgivereMal.removeAll()

        val sykefraværstilfeller = sykefraværstilfellerFor(context)
        // Klassiske enkelt-AG-enkelt-periode-tester (bl.a. HentPersonE2ETest) forventer at
        // periode-/inntektsmalens egne datoer (2024-12-xx) blir stående urørt. Når testen faktisk
        // bruker flere arbeidsgivere/perioder (LocalApp/Testpersonfabrikk-scenarioer) må vi derimot
        // bruke de virkelige datoene som er satt på hver vedtaksperiode.
        val brukReelleDatoer = context.arbeidsgivere.size > 1 || context.vedtaksperioder.size > 1

        context.arbeidsgivere.forEach { arbeidsgiver ->
            val arbeidsgiverNode = arbeidsgiverMal.deepCopy() as ObjectNode
            arbeidsgiverNode.put("organisasjonsnummer", arbeidsgiver.organisasjonsnummer)

            val perioder = arbeidsgiverNode.at(JsonPointer.compile("/generasjoner/0/perioder")) as ArrayNode
            perioder.removeAll()

            context.vedtaksperioderFor(arbeidsgiver).filter { it.spleisBehandlingId != null }.forEach { vedtaksperiode ->
                val periodeNode = periodeMal.deepCopy() as ObjectNode
                if (brukReelleDatoer) {
                    settVerdi(periodeNode, "/fom", vedtaksperiode.fom.toString())
                    settVerdi(periodeNode, "/tom", vedtaksperiode.tom.toString())
                }
                settVerdi(periodeNode, "/behandlingId", vedtaksperiode.spleisBehandlingId.toString())
                settVerdi(periodeNode, "/vedtaksperiodeId", vedtaksperiode.vedtaksperiodeId.toString())
                settVerdi(periodeNode, "/vilkarsgrunnlagId", context.vilkårsgrunnlagId.toString())
                settVerdi(periodeNode, "/utbetaling/id", vedtaksperiode.utbetalingId.toString())
                settVerdi(
                    periodeNode,
                    "/utbetaling/arbeidsgiveroppdrag/simulering/perioder/0/utbetalinger/0/utbetalesTilId",
                    arbeidsgiver.organisasjonsnummer,
                )
                settVerdi(
                    periodeNode,
                    "/utbetaling/arbeidsgiveroppdrag/simulering/perioder/0/utbetalinger/0/detaljer/0/refunderesOrgNr",
                    arbeidsgiver.organisasjonsnummer,
                )
                settVerdi(
                    periodeNode,
                    "/utbetaling/arbeidsgiveroppdrag/simulering/perioder/0/utbetalinger/0/utbetalesTilNavn",
                    arbeidsgiver.navn,
                )
                settVerdi(periodeNode, "/inntekter/0/inntektskilde", arbeidsgiver.organisasjonsnummer)
                perioder.add(periodeNode)
            }

            val ghostPerioder = arbeidsgiverNode.at(JsonPointer.compile("/ghostPerioder")) as ArrayNode
            ghostPerioder.removeAll()
            if (context.vedtaksperioderFor(arbeidsgiver).isEmpty()) {
                sykefraværstilfeller.forEach { tilfelle ->
                    ghostPerioder.addObject().apply {
                        put("fom", tilfelle.fom.toString())
                        put("tom", tilfelle.tom.toString())
                        put("skjaeringstidspunkt", tilfelle.skjæringstidspunkt.toString())
                    }
                }
            }

            arbeidsgivereMal.add(arbeidsgiverNode)
        }

        val vilkårsgrunnlagMal = mal.at(JsonPointer.compile("/data/person/vilkarsgrunnlag/0")) as ObjectNode
        settVerdi(vilkårsgrunnlagMal, "/id", context.vilkårsgrunnlagId.toString())

        val realeArbeidsgivere = context.arbeidsgivere.filterNot { context.vedtaksperioderFor(it).isEmpty() }
        val inntektMal = vilkårsgrunnlagMal.at(JsonPointer.compile("/inntekter/0")).deepCopy() as ObjectNode
        val refusjonMal = vilkårsgrunnlagMal.at(JsonPointer.compile("/arbeidsgiverrefusjoner/0")).deepCopy() as ObjectNode

        val inntekter = vilkårsgrunnlagMal.at(JsonPointer.compile("/inntekter")) as ArrayNode
        inntekter.removeAll()
        val refusjoner = vilkårsgrunnlagMal.at(JsonPointer.compile("/arbeidsgiverrefusjoner")) as ArrayNode
        refusjoner.removeAll()

        realeArbeidsgivere.forEach { arbeidsgiver ->
            inntekter.add(
                (inntektMal.deepCopy() as ObjectNode).apply {
                    put("arbeidsgiver", arbeidsgiver.organisasjonsnummer)
                    put("deaktivert", false)
                },
            )
            refusjoner.add((refusjonMal.deepCopy() as ObjectNode).apply { put("arbeidsgiver", arbeidsgiver.organisasjonsnummer) })
        }

        wireMockServer.stubFor(
            post("/graphql")
                .withRequestBody(matchingJsonPath("\$.variables[?(@.fnr == '${person.fødselsnummer}')]"))
                .willReturn(okJson(mal.toPrettyString())),
        )
    }

    /**
     * Et sykefraværstilfelle er den sammenhengende perioden med sykdom på tvers av arbeidsgivere.
     * Opphold i sykdommen (uansett hvilken arbeidsgiver) starter et nytt tilfelle. Brukes til å
     * utlede ghost-perioder for arbeidsgivere uten egne vedtaksperioder: en ghost-arbeidsgiver skal
     * ha én ghost-periode per sykefraværstilfelle, som strekker seg over hele tilfellet.
     */
    private data class Sykefraværstilfelle(
        val fom: java.time.LocalDate,
        val tom: java.time.LocalDate,
        val skjæringstidspunkt: java.time.LocalDate,
    )

    private fun sykefraværstilfellerFor(context: TestContext): List<Sykefraværstilfelle> =
        context.vedtaksperioder
            .sortedBy(Vedtaksperiode::fom)
            .fold(mutableListOf<MutableList<Vedtaksperiode>>()) { tilfeller, periode ->
                val gjeldende = tilfeller.lastOrNull()
                if (gjeldende != null && !periode.fom.isAfter(gjeldende.maxOf(Vedtaksperiode::tom).plusDays(1))) {
                    gjeldende.add(periode)
                } else {
                    tilfeller.add(mutableListOf(periode))
                }
                tilfeller
            }.map { perioderITilfellet ->
                Sykefraværstilfelle(
                    fom = perioderITilfellet.minOf(Vedtaksperiode::fom),
                    tom = perioderITilfellet.maxOf(Vedtaksperiode::tom),
                    skjæringstidspunkt = perioderITilfellet.minOf(Vedtaksperiode::fom),
                )
            }

    private fun settVerdi(
        jsonNode: JsonNode,
        pointer: String,
        verdi: String,
    ) {
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

    internal fun spleisReberegnerPerioden(
        testContext: TestContext,
        vedtaksperiode: Vedtaksperiode,
    ) {
        spleisReberegnerPerioden(testContext.person, vedtaksperiode)
        spleisForkasterGammelUtbetaling(testContext.person, vedtaksperiode.arbeidsgiver, vedtaksperiode)
        spleisLagerNyUtbetalingForVedtaksperiode(testContext.person, vedtaksperiode.arbeidsgiver, vedtaksperiode)
    }

    private fun spleisReberegnerPerioden(
        person: Person,
        vedtaksperiode: Vedtaksperiode,
    ) {
        val melding =
            Meldingsbygger.byggVedtaksperiodeEndret(
                vedtaksperiode = vedtaksperiode,
                person = person,
                forrigeTilstand = "AVVENTER_GODKJENNING",
                gjeldendeTilstand = "AVVENTER_BLOKKERENDE_PERIODE",
            )
        rapidsConnection.publish(person.fødselsnummer, melding)
    }

    internal fun spleisForkasterPerioden(
        testContext: TestContext,
        vedtaksperiode: Vedtaksperiode,
    ) {
        rapidsConnection.publish(
            testContext.person.fødselsnummer,
            Meldingsbygger.byggVedtaksperiodeForkastet(vedtaksperiode, testContext.person),
        )
    }

    private fun spleisForkasterGammelUtbetaling(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        vedtaksperiode: Vedtaksperiode,
    ) {
        val melding =
            Meldingsbygger.byggUtbetalingEndret(
                vedtaksperiode = vedtaksperiode,
                person = person,
                arbeidsgiver = arbeidsgiver,
                forrigeStatus = "IKKE_UTBETALT",
                gjeldendeStatus = "FORKASTET",
            )
        vedtaksperiode.utbetalingId = null
        rapidsConnection.publish(person.fødselsnummer, melding)
    }

    private fun spleisLagerNyUtbetalingForVedtaksperiode(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        vedtaksperiode: Vedtaksperiode,
    ) {
        vedtaksperiode.nyUtbetaling()
        val utbetalingEndret =
            Meldingsbygger.byggUtbetalingEndret(
                vedtaksperiode = vedtaksperiode,
                person = person,
                arbeidsgiver = arbeidsgiver,
                forrigeStatus = "NY",
                gjeldendeStatus = "IKKE_UTBETALT",
            )
        val vedtaksperiodeNyUtbetaling =
            Meldingsbygger.byggVedtaksperiodeNyUtbetaling(
                vedtaksperiode = vedtaksperiode,
                person = person,
                arbeidsgiver = arbeidsgiver,
            )
        rapidsConnection.publish(person.fødselsnummer, utbetalingEndret)
        rapidsConnection.publish(person.fødselsnummer, vedtaksperiodeNyUtbetaling)
    }

    private fun skalSvarePåMeldinger(jsonNode: JsonNode) = !denylisteForPersoner.contains(jsonNode["fødselsnummer"].asString())

    fun ikkeSvarPåMeldingerFor(person: Person) {
        denylisteForPersoner.add(person.fødselsnummer)
    }

    fun svarPåMeldingerFor(person: Person) {
        denylisteForPersoner.remove(person.fødselsnummer)
    }

    private inner class GodkjenningsbehovløsningRiver(
        rapidsConnection: RapidsConnection,
    ) : River.PacketListener {
        init {
            River(rapidsConnection)
                .precondition(::precondition)
                .register(this)
        }

        override fun onPacket(
            packet: JsonMessage,
            context: MessageContext,
            metadata: MessageMetadata,
            meterRegistry: MeterRegistry,
        ) {
            val jsonNode = objectMapper.readTree(packet.toJson())
            if (!skalSvarePåMeldinger(jsonNode)) {
                logg.warn("SpleisStub ignorerer melding:\n${packet.toJson()}")
                return
            }
            val fødselsnummer = jsonNode["fødselsnummer"].asString()
            val testContext =
                contextForPerson(fødselsnummer)
            val vedtaksperiodeId = UUID.fromString(jsonNode["vedtaksperiodeId"].asString())
            val vedtaksperiode =
                testContext.vedtaksperioder.find { it.vedtaksperiodeId == vedtaksperiodeId }
                    ?: error("Fant ikke igjen vedtaksperiode $vedtaksperiodeId i context for person $fødselsnummer")

            val godkjent = jsonNode["@løsning"]["Godkjenning"]["godkjent"].asBoolean()
            if (godkjent) {
                spleisLukkerBehandlingen(vedtaksperiode, testContext.person)
                utbetalingSkjer(vedtaksperiode, testContext.person, vedtaksperiode.arbeidsgiver)
                spleisAvslutterPerioden(vedtaksperiode, testContext.person, vedtaksperiode.arbeidsgiver)
            } else {
                spleisForkasterPerioden(testContext, vedtaksperiode)
            }
        }

        private fun precondition(jsonMessage: JsonMessage) {
            jsonMessage.requireAll("@behov", listOf("Godkjenning"))
            jsonMessage.requireKey("@løsning")
        }

        private fun utbetalingSkjer(
            vedtaksperiode: Vedtaksperiode,
            person: Person,
            arbeidsgiver: Arbeidsgiver,
        ) {
            rapidsConnection.publish(
                person.fødselsnummer,
                Meldingsbygger.byggUtbetalingEndret(
                    vedtaksperiode = vedtaksperiode,
                    person = person,
                    arbeidsgiver = arbeidsgiver,
                    forrigeStatus = "SENDT",
                    gjeldendeStatus = "UTBETALT",
                ),
            )
        }

        private fun spleisAvslutterPerioden(
            vedtaksperiode: Vedtaksperiode,
            person: Person,
            arbeidsgiver: Arbeidsgiver,
        ) {
            rapidsConnection.publish(
                person.fødselsnummer,
                Meldingsbygger.byggAvsluttetMedVedtak(person, arbeidsgiver, vedtaksperiode),
            )
        }

        private fun spleisLukkerBehandlingen(
            vedtaksperiode: Vedtaksperiode,
            person: Person,
        ) {
            rapidsConnection.publish(
                person.fødselsnummer,
                Meldingsbygger.byggBehandlingLukket(person, vedtaksperiode),
            )
        }
    }

    private inner class OverstyringRiver(
        rapidsConnection: RapidsConnection,
    ) : River.PacketListener {
        init {
            River(rapidsConnection)
                .precondition(::precondition)
                .register(this)
        }

        override fun onPacket(
            packet: JsonMessage,
            context: MessageContext,
            metadata: MessageMetadata,
            meterRegistry: MeterRegistry,
        ) {
            val jsonNode = objectMapper.readTree(packet.toJson())
            val skjønnsfastsatteArbeidsgivere = jsonNode["arbeidsgivere"]
            val skjæringstidspunkt = jsonNode["skjæringstidspunkt"].asLocalDate()
            val organisasjonsnummer = skjønnsfastsatteArbeidsgivere.firstOrNull()?.get("organisasjonsnummer")?.asString()
            val fødselsnummer = jsonNode["fødselsnummer"].asString()
            val testContext =
                contextForPerson(fødselsnummer)
            val vedtaksperiode =
                testContext.vedtaksperioder
                    .filter { it.skjæringstidspunkt == skjæringstidspunkt }
                    .let { kandidater ->
                        if (organisasjonsnummer != null) {
                            kandidater.find { it.arbeidsgiver.organisasjonsnummer == organisasjonsnummer } ?: kandidater.firstOrNull()
                        } else {
                            kandidater.firstOrNull()
                        }
                    }
                    ?: error("Fant ikke vedtaksperiode med skjæringstidspunkt $skjæringstidspunkt i context for person $fødselsnummer")
            spleisReberegnerPerioden(testContext, vedtaksperiode)
            spleisSenderGodkjenningsbehovMedSkjønnsfastsattSykepengegrunnlag(
                person = testContext.person,
                arbeidsgiver = vedtaksperiode.arbeidsgiver,
                vedtaksperiode = vedtaksperiode,
                vilkårsgrunnlagId = testContext.vilkårsgrunnlagId,
                skjønnsfastsatteArbeidsgivereJson = skjønnsfastsatteArbeidsgivere,
            )
        }

        private fun precondition(jsonMessage: JsonMessage) {
            jsonMessage.requireValue("@event_name", "skjønnsmessig_fastsettelse")
        }

        private fun spleisSenderGodkjenningsbehovMedSkjønnsfastsattSykepengegrunnlag(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            vilkårsgrunnlagId: UUID,
            skjønnsfastsatteArbeidsgivereJson: JsonNode,
        ) {
            vedtaksperiode.sykepengegrunnlagsfakta =
                Sykepengegrunnlagsfakta(
                    skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt,
                    fastsatt = Sykepengegrunnlagsfakta.FastsattType.EtterSkjønn,
                    arbeidsgivere =
                        skjønnsfastsatteArbeidsgivereJson.toList().map {
                            Sykepengegrunnlagsfakta.SkjønnsfastsattArbeidsgiver(
                                organisasjonsnummer = it["organisasjonsnummer"].asString(),
                                omregnetÅrsinntekt = 123456.7,
                                skjønnsfastsatt = it["årlig"].asDouble(),
                            )
                        },
                )
            val melding =
                Meldingsbygger.byggGodkjenningsbehov(
                    person = person,
                    arbeidsgiver = arbeidsgiver,
                    vedtaksperiode = vedtaksperiode,
                    vilkårsgrunnlagId = vilkårsgrunnlagId,
                    sykepengegrunnlagsfakta = vedtaksperiode.sykepengegrunnlagsfakta,
                )
            rapidsConnection.publish(person.fødselsnummer, melding)
        }
    }

    private fun contextForPerson(fødselsnummer: String): TestContext =
        (
            contextsForFødselsnummer[fødselsnummer]
                ?: error("Ikke initialisert med context for person $fødselsnummer")
        )
}
