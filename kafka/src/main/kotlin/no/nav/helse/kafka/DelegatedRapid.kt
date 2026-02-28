package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.FailedMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.github.navikt.tbd_libs.rapids_and_rivers_api.SentMessage
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.logg.medMdc
import no.nav.helse.spesialist.kafka.objectMapper

internal class DelegatedRapid(
    private val rapidsConnection: RapidsConnection,
    private val beforeRiversAction: () -> Unit,
    private val skalBehandleMelding: (String) -> Boolean,
    private val afterRiversAction: (String) -> Unit,
    private val errorAction: (Exception, String) -> Unit,
) : RapidsConnection(),
    RapidsConnection.MessageListener {
    init {
        rapidsConnection.register(this)
    }

    override fun rapidName(): String = "Spesialist"

    override fun onMessage(
        message: String,
        context: MessageContext,
        metadata: MessageMetadata,
        metrics: MeterRegistry,
    ) {
        medMdcBasertPåMelding(message) {
            try {
                beforeRiversAction()
                if (skalBehandleMelding(message)) notifyMessage(message, context, metadata, metrics)
                afterRiversAction(message)
            } catch (err: Exception) {
                errorAction(err, message)
                throw err
            }
        }
    }

    private fun medMdcBasertPåMelding(
        message: String,
        block: () -> Unit,
    ) {
        val jsonNode =
            runCatching { objectMapper.readTree(message) }.getOrElse { e ->
                loggError("Klarte ikke tolke melding som JSON for å sette MDC", e)
                return block()
            }
        medMdc(
            jsonNode.safelyGetText("@id")?.let { MdcKey.MELDING_ID to it },
            jsonNode.safelyGetMeldingsnavn()?.let { MdcKey.MELDINGNAVN to it },
            jsonNode.safelyGetText("behandlingId")?.let { MdcKey.SPLEIS_BEHANDLING_ID to it },
            jsonNode.safelyGetText("vedtaksperiodeId")?.let { MdcKey.VEDTAKSPERIODE_ID to it },
            jsonNode.safelyGetText("fødselsnummer")?.let { MdcKey.IDENTITETSNUMMER to it },
        ) {
            block()
        }
    }

    private fun JsonNode.safelyGetMeldingsnavn(): String? =
        safelyGetText("@event_name")?.let {
            when (it) {
                "behov" ->
                    "behov: " +
                        this["@behov"]
                            ?.takeUnless(JsonNode::isMissingOrNull)
                            ?.joinToString(
                                separator = ", ",
                                prefix = "[",
                                postfix = "]",
                                transform = JsonNode::textValue,
                            ).orEmpty()

                else -> it
            }
        }

    private fun JsonNode.safelyGetText(key: String): String? = get(key)?.takeUnless(JsonNode::isMissingOrNull)?.textValue()

    override fun publish(message: String) {
        rapidsConnection.publish(message)
    }

    override fun publish(
        key: String,
        message: String,
    ) {
        rapidsConnection.publish(key, message)
    }

    override fun publish(messages: List<OutgoingMessage>): Pair<List<SentMessage>, List<FailedMessage>> = rapidsConnection.publish(messages)

    override fun start() = throw IllegalStateException()

    override fun stop() = throw IllegalStateException()
}
