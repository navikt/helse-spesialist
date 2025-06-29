package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers_api.FailedMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.KeyMessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.github.navikt.tbd_libs.rapids_and_rivers_api.SentMessage
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.util.concurrent.ConcurrentHashMap

class LoopbackTestRapid : RapidsConnection() {
    private val meterRegistry: MeterRegistry = SimpleMeterRegistry()
    private val messageMetadata = MessageMetadata("test.message", -1, -1, null, emptyMap())
    private val meldingsloggForFødselsnummer = ConcurrentHashMap<String, MutableList<JsonNode>>()

    fun meldingslogg(fødselsnummer: String) = meldingsloggForFødselsnummer[fødselsnummer] ?: emptyList()

    override fun publish(message: String) {
        error("Ikke implementert, bruk publish med fødselsnummer for å kunne kjøre med flere tråder.")
    }

    override fun publish(key: String, message: String) {
        meldingsloggForFødselsnummer.computeIfAbsent(key) { mutableListOf<JsonNode>() }
            .add(objectMapper.readTree(message))
        notifyMessage(message, KeyMessageContext(this, key), messageMetadata, meterRegistry)
    }

    override fun publish(messages: List<OutgoingMessage>): Pair<List<SentMessage>, List<FailedMessage>> =
        messages.mapIndexed { index, message ->
            val key = message.key
            if (key != null) {
                publish(key, message.body)
            } else {
                error("Alle meldinger som skal publiseres må ha nøkkel, slik at testene kan kjøre med flere tråder.")
            }
            SentMessage(
                index = index,
                message = message,
                partition = key.hashCode(),
                offset = index.toLong()
            )
        } to emptyList()

    override fun rapidName(): String = javaClass.simpleName
    override fun start() {}
    override fun stop() {}
}
