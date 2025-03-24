package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.e2etests.LoopbackTestRapid
import java.time.LocalDateTime
import java.util.UUID

class BehovLøserStub(private val løsere: List<AbstractBehovLøser>) : River.PacketListener {
    private val løserPerBehov = løsere.associateBy { it.behov }
    private val behovViKanLøse = løserPerBehov.keys

    private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun registerOn(rapid: LoopbackTestRapid) {
        River(rapid)
            .precondition(::precondition)
            .register(this)
    }

    private fun precondition(jsonMessage: JsonMessage) {
        jsonMessage.require("@behov") { behov ->
            val behovIMelding = behov.map { it.asText() }.toSet()
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

        val innkommendeMeldingMap: Map<String, Any?> =
            objectMapper.readValue(
                packet.toJson(),
                object : TypeReference<Map<String, Any?>>() {}
            )
        val svarmelding = objectMapper.writeValueAsString(
            innkommendeMeldingMap + modifikasjoner(jsonNode)
        )
        logg.info("${this.javaClass.simpleName} publiserer simulert svarmelding fra ekstern tjeneste: $svarmelding")
        context.publish(svarmelding)
    }

    private fun modifikasjoner(jsonNode: JsonNode): Map<String, Any> {
        val behovliste = jsonNode["@behov"].map { it.asText() }
        return mapOf(
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
            "@løsning" to behovliste.associateWith { behov ->
                løsere.find { it.behov == behov }?.løsning(jsonNode[behov])
                    ?: error("Skulle ikke kommet hit! Har ikke løser for behov: $behov")
            },
            "@final" to true,
            "@besvart" to LocalDateTime.now()
        )
    }
}
