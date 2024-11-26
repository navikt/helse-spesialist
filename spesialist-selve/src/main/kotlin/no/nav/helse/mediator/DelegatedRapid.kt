package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

internal class DelegatedRapid(
    private val rapidsConnection: RapidsConnection,
    private val beforeRiversAction: () -> Unit,
    private val skalBehandleMelding: (String) -> Boolean,
    private val afterRiversAction: (String) -> Unit,
    private val errorAction: (Exception, String) -> Unit,
) : RapidsConnection(), RapidsConnection.MessageListener {
    init {
        rapidsConnection.register(this)
    }

    override fun rapidName(): String {
        return "Spesialist"
    }

    override fun onMessage(
        message: String,
        context: MessageContext,
        metadata: MessageMetadata,
        metrics: MeterRegistry,
    ) {
        try {
            beforeRiversAction()
            if (skalBehandleMelding(message)) notifyMessage(message, context, metadata, metrics)
            afterRiversAction(message)
        } catch (err: Exception) {
            errorAction(err, message)
            throw err
        }
    }

    override fun publish(message: String) {
        rapidsConnection.publish(message)
    }

    override fun publish(
        key: String,
        message: String,
    ) {
        rapidsConnection.publish(key, message)
    }

    override fun start() = throw IllegalStateException()

    override fun stop() = throw IllegalStateException()
}
