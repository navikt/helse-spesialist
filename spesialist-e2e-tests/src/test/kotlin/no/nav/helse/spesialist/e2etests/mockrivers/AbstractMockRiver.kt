package no.nav.helse.spesialist.e2etests.mockrivers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.e2etests.SimulatingTestRapid

abstract class AbstractMockRiver : River.PacketListener {
    abstract fun precondition(jsonMessage: JsonMessage)

    abstract fun responseFor(json: JsonNode): String

    fun registerOn(rapid: SimulatingTestRapid) {
        River(rapid)
            .precondition(::precondition)
            .register(this)
    }

    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val response = responseFor(objectMapper.readTree(packet.toJson()))
        logg.info("${this.javaClass.simpleName} publiserer simulert svarmelding fra ekstern tjeneste: $response")
        context.publish(response)
    }
}
