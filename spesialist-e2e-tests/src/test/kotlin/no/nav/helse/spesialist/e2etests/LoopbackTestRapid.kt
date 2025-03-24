package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers_api.KeyMessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

class LoopbackTestRapid : RapidsConnection() {
    private val meterRegistry: MeterRegistry = SimpleMeterRegistry()
    private val messageMetadata = MessageMetadata("test.message", -1, -1, null, emptyMap())
    val meldingslogg = mutableListOf<JsonNode>()

    override fun publish(message: String) {
        meldingslogg.add(objectMapper.readTree(message))
        notifyMessage(message, this, messageMetadata, meterRegistry)
    }

    override fun publish(key: String, message: String) {
        meldingslogg.add(objectMapper.readTree(message))
        notifyMessage(message, KeyMessageContext(this, key), messageMetadata, meterRegistry)
    }

    override fun rapidName(): String = javaClass.simpleName
    override fun start() {}
    override fun stop() {}
}
