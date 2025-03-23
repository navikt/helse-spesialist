package no.nav.helse.spesialist.e2etests.mockrivers

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


abstract class AbstractBehovMockRiver(private vararg val behov: String) : River.PacketListener {
    abstract fun løsning(json: JsonNode): Map<String, Any?>

    protected val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun registerOn(rapid: LoopbackTestRapid) {
        River(rapid)
            .precondition(::precondition)
            .register(this)
    }

    private fun precondition(jsonMessage: JsonMessage) {
        jsonMessage.requireAll("@behov", behov.toList())
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

    private fun modifikasjoner(jsonNode: JsonNode) =
        mapOf(
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "system_read_count" to 0,
            "system_participating_services" to
                    (jsonNode["system_participating_services"] as ArrayNode).toMutableList() +
                    mapOf(
                        "id" to UUID.randomUUID(),
                        "time" to LocalDateTime.now(),
                        "service" to "mockriver-${javaClass.simpleName}",
                        "instance" to "mockriver-${javaClass.simpleName}",
                        "image" to "mockriver-${javaClass.simpleName}",
                    ),
            "@forårsaket_av" to mapOf(
                "id" to jsonNode["@id"],
                "opprettet" to jsonNode["@opprettet"],
                "event_name" to jsonNode["@event_name"],
                "behov" to jsonNode["@behov"]
            ),
            "@løsning" to løsning(jsonNode),
            "@final" to true,
            "@besvart" to LocalDateTime.now()
        )
}
