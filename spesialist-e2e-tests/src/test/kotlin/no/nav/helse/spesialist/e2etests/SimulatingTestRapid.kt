package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers_api.KeyMessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

class SimulatingTestRapid : RapidsConnection() {
    private val _messageLog = mutableListOf<JsonNode>()
    val messageLog: List<JsonNode> = _messageLog

    private val meterRegistry: MeterRegistry = SimpleMeterRegistry()
    private val messageMetadata = MessageMetadata("test.message", -1, -1, null, emptyMap())
    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private fun addToLog(message: String) {
        _messageLog.add(objectMapper.readTree(message))
    }

    override fun publish(message: String) {
        addToLog(message)
        notifyMessage(message, this, messageMetadata, meterRegistry)
    }

    override fun publish(key: String, message: String) {
        addToLog(message)
        notifyMessage(message, KeyMessageContext(this, key), messageMetadata, meterRegistry)
    }

    override fun rapidName(): String = javaClass.simpleName
    override fun start() {}
    override fun stop() {}
}
