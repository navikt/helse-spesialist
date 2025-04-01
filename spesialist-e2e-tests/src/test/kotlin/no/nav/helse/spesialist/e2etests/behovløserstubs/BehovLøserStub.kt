package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.e2etests.LoopbackTestRapid
import no.nav.helse.spesialist.e2etests.VårArbeidsgiver
import no.nav.helse.spesialist.e2etests.VårTestPerson
import no.nav.helse.spesialist.e2etests.objectMapper
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BehovLøserStub(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    private val løsereForFødselsnummer = ConcurrentHashMap<String, List<AbstractBehovLøser>>()
    private fun løserPerBehov(fødselsnummer: String) =
        løsereForFødselsnummer(fødselsnummer).associateBy(AbstractBehovLøser::behov)

    private val behovlisteForFødselsnummer = ConcurrentHashMap<String, MutableList<JsonNode>>()
    private fun behovliste(fødselsnummer: String) =
        behovlisteForFødselsnummer.computeIfAbsent(fødselsnummer) { mutableListOf() }

    fun init(person: VårTestPerson, arbeidsgiver: VårArbeidsgiver) {
        løsereForFødselsnummer[person.fødselsnummer] = listOf(
            ArbeidsforholdBehovLøser(),
            ArbeidsgiverinformasjonBehovLøser(),
            AvviksvurderingBehovLøser(),
            EgenAnsattBehovLøser(),
            HentEnhetBehovLøser(),
            HentInfotrygdutbetalingerBehovLøser(arbeidsgiver.organisasjonsnummer),
            HentPersoninfoV2BehovLøser(person),
            InntekterForSykepengegrunnlagBehovLøser(arbeidsgiver.organisasjonsnummer),
            RisikovurderingBehovLøser(),
            FullmaktBehovLøser(),
            VergemålBehovLøser(),
            ÅpneOppgaverBehovLøser(),
        )
    }

    fun registerOn(rapid: LoopbackTestRapid) {
        River(rapid)
            .precondition(::precondition)
            .register(this)
    }

    private fun precondition(jsonMessage: JsonMessage) {
        jsonMessage.require("@behov") { behov ->
            jsonMessage.interestedIn("fødselsnummer")
            val fødselsnummer = jsonMessage["fødselsnummer"].asText()
            val behovIMelding = behov.map { it.asText() }.toSet()
            val behovViKanLøse = løserPerBehov(fødselsnummer).keys
            val behovIMeldingenViIkkeKanLøse = behovIMelding.filterNot { it in behovViKanLøse }
            if (behovIMeldingenViIkkeKanLøse.isNotEmpty()) error("Kan ikke løse behov $behovIMeldingenViIkkeKanLøse, ignorerer")
        }
        jsonMessage.forbid("@løsning")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val jsonNode = objectMapper.readTree(packet.toJson())
        val fødselsnummer = jsonNode["fødselsnummer"].asText()
        behovliste(fødselsnummer).add(jsonNode)
        besvarMelding(
            jsonNode = jsonNode,
            løserPerBehov = løserPerBehov(fødselsnummer = fødselsnummer),
            context = context
        )
    }

    private fun besvarMelding(
        jsonNode: JsonNode,
        løserPerBehov: Map<String, AbstractBehovLøser>,
        context: MessageContext
    ) {
        val innkommendeMeldingMap: Map<String, Any?> =
            objectMapper.readValue(
                objectMapper.writeValueAsString(jsonNode),
                object : TypeReference<Map<String, Any?>>() {}
            )
        val svarmelding = objectMapper.writeValueAsString(
            innkommendeMeldingMap + modifikasjoner(
                jsonNode = jsonNode,
                løserPerBehov = løserPerBehov
            )
        )
        logg.info("${this.javaClass.simpleName} publiserer simulert svarmelding fra ekstern tjeneste: $svarmelding")
        context.publish(svarmelding)
    }

    fun besvarIgjen(fødselsnummer: String, behov: String) {
        val sisteBehov = behovliste(fødselsnummer).findLast { behov in it["@behov"].map { it.asText() } }
            ?: error("Fant ikke behov $behov i behovliste")
        besvarMelding(
            jsonNode = sisteBehov,
            løserPerBehov = løserPerBehov(fødselsnummer = sisteBehov["fødselsnummer"].asText()),
            context = rapidsConnection
        )
    }

    private fun modifikasjoner(
        jsonNode: JsonNode,
        løserPerBehov: Map<String, AbstractBehovLøser>
    ) = mapOf(
        "@id" to UUID.randomUUID(),
        "@opprettet" to LocalDateTime.now(),
        "system_read_count" to 0,
        "system_participating_services" to
                (jsonNode["system_participating_services"] as ArrayNode).toMutableList() +
                mapOf(
                    "id" to UUID.randomUUID(),
                    "time" to LocalDateTime.now(),
                    "service" to javaClass.simpleName,
                    "instance" to javaClass.simpleName,
                    "image" to javaClass.simpleName,
                ),
        "@forårsaket_av" to mapOf(
            "id" to jsonNode["@id"],
            "opprettet" to jsonNode["@opprettet"],
            "event_name" to jsonNode["@event_name"],
            "behov" to jsonNode["@behov"]
        ),
        "@løsning" to jsonNode["@behov"].map { it.asText() }.associateWith { behov ->
            løserPerBehov[behov]?.løsning(jsonNode[behov])
                ?: error("Skulle ikke kommet hit! Har ikke løser for behov: $behov")
        },
        "@final" to true,
        "@besvart" to LocalDateTime.now()
    )

    inline fun <reified T : AbstractBehovLøser> finnLøser(fødselsnummer: String): T =
        løsereForFødselsnummer(fødselsnummer).filterIsInstance<T>().first()

    fun løsereForFødselsnummer(fødselsnummer: String) =
        løsereForFødselsnummer[fødselsnummer] ?: error("Ikke satt opp med meldinger for fødselsnummer $fødselsnummer")
}
